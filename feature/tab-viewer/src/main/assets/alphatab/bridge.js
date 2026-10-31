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

  // Los colores llegan en la URL desde Kotlin (ScoreSheetStyle en core/designsystem), que es la
  // única fuente. Los valores por defecto son los del diseño Taller, para abrir la página suelta.
  var params = new URLSearchParams(location.search);
  function color(name, fallback) {
    var value = params.get(name);
    return /^[0-9a-fA-F]{6}$/.test(value || "") ? "#" + value : fallback;
  }
  function softColor(hex, alpha) {
    var n = parseInt(hex.slice(1), 16);
    return "rgba(" + (n >> 16 & 255) + ", " + (n >> 8 & 255) + ", " + (n & 255) + ", " + alpha + ")";
  }
  var accent = color("accent", "#5B3FD1");
  var accentAlpha = parseFloat(params.get("accentAlpha"));
  var theme = {
    sheet: color("bg", "#FFFFFF"),
    accent: accent,
    accentSoft: softColor(accent, isNaN(accentAlpha) ? 0.1 : accentAlpha),
    staff: color("staff", "#A59FB8"),
    bar: color("bar", "#6C6585"),
    glyph: color("glyph", "#1F1A33"),
    secondary: color("secondary", "#6C6585"),
    info: color("info", "#1F1A33"),
    barNumber: color("barNumber", "#5B3FD1")
  };
  var rootStyle = document.documentElement.style;
  rootStyle.setProperty("--sheet", theme.sheet);
  rootStyle.setProperty("--accent", theme.accent);
  rootStyle.setProperty("--accent-soft", theme.accentSoft);

  if (typeof alphaTab === "undefined") {
    report("No se pudo cargar AlphaTab");
    return;
  }

  var viewport = document.getElementById("viewport");
  var api = new alphaTab.AlphaTabApi(document.getElementById("score"), {
    core: {
      fontDirectory: base + "font/",
      scriptFile: base + "alphaTab.min.js",
      useWorkers: true,
      // Se dibuja toda la partitura de una vez, sin esperar a que cada trozo sea visible
      // (IntersectionObserver): así el pedal y el cursor nunca llegan a un trozo en blanco.
      enableLazyLoading: false
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
      enableAnimatedBeatCursor: true,
      // Tocar un compás mueve el cursor allí.
      enableUserInteraction: true
    }
  });

  var lastSentSecond = -1;
  var loopStart = null; // MasterBarTickLookup del punto A, mientras falta marcar B
  var activeRange = null; // { startTick, endTick } del bucle A-B activo

  api.scoreLoaded.on(function (score) {
    // Nombre de reserva para pistas sin nombre. Igual que los textos de la app: inglés si el
    // sistema está en inglés y español en los demás casos.
    var trackLabel = /^en\b/i.test(navigator.language || "") ? "Track " : "Pista ";
    var names = score.tracks.map(function (t) { return t.name || (trackLabel + (t.index + 1)); });
    native.onScoreLoaded(score.title || "", score.artist || "", names.join(SEP));
  });
  api.renderFinished.on(function () { native.onRenderFinished(); });
  api.postRenderFinished.on(function () {
    // Al cambiar de pista se vuelve a dibujar la partitura: hay que volver a marcar el A-B
    // (postRenderFinished llega cuando ya se conocen las posiciones de cada compás).
    if (activeRange) highlightRange(activeRange);
    // Diagnóstico en Logcat (WebView): tamaño del visor y trozos de partitura dibujados.
    console.info("Mi Tablaturas: visor " + viewport.clientWidth + "x" + viewport.clientHeight +
      ", svg " + document.querySelectorAll("#score svg").length);
  });
  api.playerReady.on(function () { native.onPlayerReady(); });
  api.playerStateChanged.on(function (e) { native.onPlayerStateChanged(e.state === 1); });
  api.playerFinished.on(function () {
    // Con «Repetir» activo AlphaTab avisa al final de cada vuelta y sigue sonando.
    if (api.isLooping) {
      native.onLoopCompleted();
    } else {
      native.onPlayerStateChanged(false);
    }
  });
  api.playerPositionChanged.on(function (e) {
    // Se limita a un evento por segundo para no saturar el puente ni recomponer de más.
    var second = Math.floor(e.currentTime / 1000);
    if (second === lastSentSecond && !e.isSeek) return;
    lastSentSecond = second;
    native.onPositionChanged(Math.round(e.currentTime), Math.round(e.endTime), Math.round(e.modifiedTempo));
  });
  api.playbackRangeChanged.on(function (e) {
    var range = e.playbackRange;
    // Tocar la partitura para mover el cursor borra la selección; el A-B solo se quita con su botón.
    if (!range && activeRange) {
      applyRange(activeRange);
      return;
    }
    activeRange = range ? { startTick: range.startTick, endTick: range.endTick } : null;
    reportRange();
  });
  api.error.on(function (e) { report(e && e.message); });

  /** Compás (MasterBarTickLookup) bajo el cursor, o null si aún no hay datos de reproducción. */
  function barAtCursor() {
    var cache = api.tickCache;
    if (!cache || !api.tracks.length) return null;
    var tracks = new Set(api.tracks.map(function (t) { return t.index; }));
    var result = cache.findBeat(tracks, api.tickPosition);
    return result ? result.masterBar : null;
  }

  /** Compás (MasterBarTickLookup) que contiene el tick, con las repeticiones ya desplegadas. */
  function barAtTick(tick) {
    var bars = api.tickCache ? api.tickCache.masterBars : [];
    for (var i = 0; i < bars.length; i++) {
      if (tick >= bars[i].start && tick < bars[i].end) return bars[i];
    }
    return null;
  }

  /** Informa a Kotlin de los compases (desde 1) del bucle; 0 si no hay punto A o B. */
  function reportRange() {
    var first = activeRange && barAtTick(activeRange.startTick);
    var last = activeRange && barAtTick(activeRange.endTick - 1);
    if (first && last) {
      native.onLoopRangeChanged(first.masterBar.index + 1, last.masterBar.index + 1);
    } else {
      native.onLoopRangeChanged(loopStart ? loopStart.masterBar.index + 1 : 0, 0);
    }
  }

  function applyRange(range) {
    api.playbackRange = range;
    highlightRange(range);
  }

  /**
   * Quita la marca del bucle. AlphaTab recuerda la última selección y la vuelve a dibujar tras
   * cada render, aunque sea de otra pista (y entonces falla); una selección de un solo pulso
   * no se dibuja, así que se deja esa en su lugar.
   */
  function resetSelection() {
    var bars = api.tracks.length ? api.tracks[0].staves[0].bars : [];
    var beat = bars.length ? bars[0].voices[0].beats[0] : null;
    if (beat) api.highlightPlaybackRange(beat, beat);
  }

  /** Marca en la partitura los compases del bucle, en la pista que se está mostrando. */
  function highlightRange(range) {
    var first = barAtTick(range.startTick);
    var last = barAtTick(range.endTick - 1);
    if (!first || !last || !api.tracks.length) return;
    var bars = api.tracks[0].staves[0].bars;
    var startBeats = bars[first.masterBar.index].voices[0].beats;
    var endBeats = bars[last.masterBar.index].voices[0].beats;
    api.highlightPlaybackRange(startBeats[0], endBeats[endBeats.length - 1]);
  }

  function scrollPage(direction) {
    viewport.scrollBy({ top: direction * viewport.clientHeight * 0.85, behavior: "smooth" });
  }

  window.tab = {
    playPause: function () { api.playPause(); },
    pause: function () { api.pause(); },
    stop: function () { api.stop(); },
    selectTrack: function (index) {
      var track = api.score && api.score.tracks[index];
      if (!track) return;
      // La marca del A-B apunta a compases de la pista anterior; se vuelve a poner al terminar.
      resetSelection();
      api.renderTracks([track]);
    },
    setSpeed: function (speed) { api.playbackSpeed = speed; },
    setLooping: function (enabled) { api.isLooping = enabled; },
    setMetronome: function (enabled) { api.metronomeVolume = enabled ? 1 : 0; },
    setCountIn: function (enabled) { api.countInVolume = enabled ? 1 : 0; },
    seek: function (ms) {
      api.timePosition = ms;
      api.scrollToCursor();
    },
    /** Punto A: el compás donde está el cursor. */
    markLoopStart: function () {
      var start = barAtCursor();
      if (!start) return;
      activeRange = null;
      api.playbackRange = null;
      resetSelection();
      loopStart = start;
      reportRange();
    },
    /** Punto B: el compás donde está el cursor; repite de A a B (en cualquier orden). */
    markLoopEnd: function () {
      var end = barAtCursor();
      if (!loopStart || !end) return;
      var first = end.start < loopStart.start ? end : loopStart;
      var last = end.start < loopStart.start ? loopStart : end;
      loopStart = null;
      activeRange = { startTick: first.start, endTick: last.end };
      applyRange(activeRange);
      api.isLooping = true;
      api.tickPosition = first.start;
      reportRange();
    },
    clearLoopRange: function () {
      loopStart = null;
      activeRange = null;
      api.playbackRange = null;
      resetSelection();
      reportRange();
    },
    scrollPage: scrollPage
  };

  // Pedal Bluetooth o teclado: los pasapáginas envían Av Pág / flechas, y Espacio reproduce.
  document.addEventListener("keydown", function (e) {
    switch (e.key) {
      case " ":
      case "Enter":
      case "MediaPlayPause":
        api.playPause();
        break;
      case "PageDown":
      case "ArrowDown":
      case "ArrowRight":
        scrollPage(1);
        break;
      case "PageUp":
      case "ArrowUp":
      case "ArrowLeft":
        scrollPage(-1);
        break;
      default:
        return;
    }
    e.preventDefault();
  });

  // Guitar Pro 3-5 guarda los textos con la codificación del ordenador donde se creó el
  // archivo, casi siempre Windows-1252 en español («Rodríguez»), y AlphaTab los lee como UTF-8.
  // Si salen caracteres irreconocibles (U+FFFD) se vuelve a leer el archivo como Windows-1252.
  var FALLBACK_ENCODING = "windows-1252";

  function scoreTexts(score) {
    var texts = [score.title, score.subTitle, score.artist, score.album, score.words, score.music,
      score.copyright, score.tab, score.instructions, score.notices];
    score.tracks.forEach(function (t) { texts.push(t.name, t.shortName); });
    return texts.join("");
  }

  function loadScore(bytes) {
    var loader = alphaTab.importer.ScoreLoader;
    var score = loader.loadScoreFromBytes(bytes, api.settings);
    if (scoreTexts(score).indexOf("\uFFFD") >= 0) {
      api.settings.importer.encoding = FALLBACK_ENCODING;
      score = loader.loadScoreFromBytes(bytes, api.settings);
    }
    api.renderScore(score);
  }

  // La partitura la sirve Kotlin en esta ruta (ver TabViewerWebView.kt).
  fetch("/score/current")
    .then(function (response) {
      if (!response.ok) throw new Error("No se encontró la partitura (" + response.status + ")");
      return response.arrayBuffer();
    })
    .then(function (buffer) { loadScore(new Uint8Array(buffer)); })
    .catch(function (e) { report(e.message); });
})();
