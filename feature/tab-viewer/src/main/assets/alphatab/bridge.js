// Puente entre AlphaTab y Kotlin.
// JS -> Kotlin: window.AndroidBridge (ver AlphaTabBridge.kt).
// Kotlin -> JS: window.tab.* (ver PlayerCommand.kt).
(function () {
  "use strict";

  var native = window.AndroidBridge;
  var SEP = "\u001F"; // separador de nombres de pista
  var base = new URL(".", document.currentScript.src).href;

  function report(message) {
    if (native) native.onError(String(message || "Error desconocido"));
  }

  window.addEventListener("error", function (e) { report(e.message); });
  window.addEventListener("unhandledrejection", function (e) { report(e.reason && e.reason.message || e.reason); });

  // Colores por diseño; deben coincidir con core/designsystem (Theme.kt).
  var THEMES = {
    atril: {
      sheet: "#FBFBFA", accent: "#2B59C3", accentSoft: "rgba(43, 89, 195, 0.12)",
      staff: "#9AA1AD", bar: "#5E6676", glyph: "#1D2330", secondary: "#5E6676", info: "#1D2330", barNumber: "#2B59C3"
    },
    escenario: {
      sheet: "#10141C", accent: "#F5A524", accentSoft: "rgba(245, 165, 36, 0.18)",
      staff: "#3A4357", bar: "#5A6478", glyph: "#E9ECF2", secondary: "#8D96A8", info: "#E9ECF2", barNumber: "#F5A524"
    },
    taller: {
      sheet: "#FFFFFF", accent: "#5B3FD1", accentSoft: "rgba(91, 63, 209, 0.10)",
      staff: "#A59FB8", bar: "#6C6585", glyph: "#1F1A33", secondary: "#6C6585", info: "#1F1A33", barNumber: "#5B3FD1"
    }
  };
  var themeName = new URLSearchParams(location.search).get("theme");
  var theme = THEMES[themeName] || THEMES.taller;
  var rootStyle = document.documentElement.style;
  rootStyle.setProperty("--sheet", theme.sheet);
  rootStyle.setProperty("--accent", theme.accent);
  rootStyle.setProperty("--accent-soft", theme.accentSoft);

  if (typeof alphaTab === "undefined") {
    report("No se pudo cargar AlphaTab");
    return;
  }

  var api = new alphaTab.AlphaTabApi(document.getElementById("score"), {
    core: {
      fontDirectory: base + "font/",
      scriptFile: base + "alphaTab.min.js",
      useWorkers: true
    },
    display: {
      layoutMode: "page",
      scale: 0.9,
      resources: {
        staffLineColor: theme.staff,
        barSeparatorColor: theme.bar,
        mainGlyphColor: theme.glyph,
        secondaryGlyphColor: theme.secondary,
        scoreInfoColor: theme.info,
        barNumberColor: theme.barNumber
      }
    },
    player: {
      playerMode: "enabledSynthesizer",
      soundFont: base + "soundfont/sonivox.sf3",
      scrollElement: "#viewport",
      enableCursor: true,
      enableAnimatedBeatCursor: true
    }
  });

  var lastSentSecond = -1;

  api.scoreLoaded.on(function (score) {
    var names = score.tracks.map(function (t) { return t.name || ("Pista " + (t.index + 1)); });
    native.onScoreLoaded(score.title || "", score.artist || "", names.join(SEP));
  });
  api.renderFinished.on(function () { native.onRenderFinished(); });
  api.playerReady.on(function () { native.onPlayerReady(); });
  api.playerStateChanged.on(function (e) { native.onPlayerStateChanged(e.state === 1); });
  api.playerFinished.on(function () { native.onPlayerStateChanged(false); });
  api.playerPositionChanged.on(function (e) {
    // Se limita a un evento por segundo para no saturar el puente ni recomponer de más.
    var second = Math.floor(e.currentTime / 1000);
    if (second === lastSentSecond && !e.isSeek) return;
    lastSentSecond = second;
    native.onPositionChanged(Math.round(e.currentTime), Math.round(e.endTime), Math.round(e.modifiedTempo));
  });
  api.error.on(function (e) { report(e && e.message); });

  window.tab = {
    playPause: function () { api.playPause(); },
    pause: function () { api.pause(); },
    stop: function () { api.stop(); },
    selectTrack: function (index) {
      var track = api.score && api.score.tracks[index];
      if (track) api.renderTracks([track]);
    },
    setSpeed: function (speed) { api.playbackSpeed = speed; },
    setLooping: function (enabled) { api.isLooping = enabled; },
    setMetronome: function (enabled) { api.metronomeVolume = enabled ? 1 : 0; }
  };

  // La partitura la sirve Kotlin en esta ruta (ver TabViewerWebView.kt).
  fetch("/score/current")
    .then(function (response) {
      if (!response.ok) throw new Error("No se encontró la partitura (" + response.status + ")");
      return response.arrayBuffer();
    })
    .then(function (buffer) { api.load(new Uint8Array(buffer)); })
    .catch(function (e) { report(e.message); });
})();
