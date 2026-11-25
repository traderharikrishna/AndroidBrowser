package com.example.browser

object Html5LocalPages {
    fun buildHtml5TestPage(): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>RelayWeb HTML5 &amp; Media Lab</title>
          <style>
            :root {
              color-scheme: dark;
              --bg: #090d14;
              --card: #121a26;
              --border: #253348;
              --cyan: #22d3ee;
              --emerald: #10b981;
              --text: #f1f5f9;
              --muted: #94a3b8;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              padding: 18px;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background: var(--bg);
              color: var(--text);
              line-height: 1.5;
            }
            .hero {
              background: linear-gradient(135deg, #101d2e, #0c1420);
              border: 1px solid var(--border);
              border-radius: 16px;
              padding: 18px;
              margin-bottom: 16px;
            }
            h1 { margin: 0 0 6px; font-size: 20px; color: var(--cyan); }
            p { margin: 0; color: var(--muted); font-size: 13px; }
            .card {
              background: var(--card);
              border: 1px solid var(--border);
              border-radius: 14px;
              padding: 16px;
              margin-bottom: 14px;
            }
            h2 { margin: 0 0 10px; font-size: 15px; }
            video, audio {
              width: 100%;
              border-radius: 10px;
              background: #000;
              outline: none;
            }
            .grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
              gap: 10px;
            }
            .cap {
              background: #0d141f;
              border: 1px solid var(--border);
              border-radius: 10px;
              padding: 10px;
              font-size: 12px;
            }
            .badge {
              display: inline-block;
              padding: 2px 8px;
              border-radius: 999px;
              font-size: 11px;
              font-weight: 700;
              margin-top: 4px;
            }
            .ok { background: rgba(16,185,129,0.18); color: var(--emerald); }
            .warn { background: rgba(245,158,11,0.18); color: #fbbf24; }
            canvas {
              width: 100%;
              height: 90px;
              border-radius: 10px;
              border: 1px solid var(--border);
              display: block;
            }
          </style>
        </head>
        <body>
          <div class="hero">
            <h1>RelayWeb HTML5 &amp; Media Lab</h1>
            <p>Live verification of WebView HTML5 Video, Audio, Canvas 2D, Web Storage, and MSE on Android.</p>
          </div>

          <div class="card">
            <h2>HTML5 Video Player (&lt;video&gt;)</h2>
            <video controls playsinline preload="metadata" poster="https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11217">
              <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" type="video/mp4" />
              Your WebView does not support HTML5 video.
            </video>
          </div>

          <div class="card">
            <h2>HTML5 Audio Player (&lt;audio&gt;)</h2>
            <audio controls preload="metadata">
              <source src="https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" type="audio/mpeg" />
              Your WebView does not support HTML5 audio.
            </audio>
          </div>

          <div class="card">
            <h2>HTML5 Canvas 2D Engine</h2>
            <canvas id="waveCanvas" width="600" height="140"></canvas>
          </div>

          <div class="card">
            <h2>HTML5 API Capabilities</h2>
            <div class="grid" id="capGrid"></div>
          </div>

          <script>
            (function() {
              var v = document.createElement('video');
              var a = document.createElement('audio');
              var storageOk = false;
              try {
                localStorage.setItem('relayweb_probe', '1');
                storageOk = localStorage.getItem('relayweb_probe') === '1';
              } catch (e) {}

              var checks = [
                { name: 'HTML5 Video', ok: !!(v.canPlayType) },
                { name: 'MP4 / H.264', ok: !!(v.canPlayType && v.canPlayType('video/mp4; codecs="avc1.42E01E"')) },
                { name: 'WebM / VP9', ok: !!(v.canPlayType && v.canPlayType('video/webm; codecs="vp9"')) },
                { name: 'HTML5 Audio', ok: !!(a.canPlayType) },
                { name: 'MP3 / AAC', ok: !!(a.canPlayType && (a.canPlayType('audio/mpeg') || a.canPlayType('audio/mp4'))) },
                { name: 'LocalStorage', ok: storageOk },
                { name: 'MediaSource (MSE)', ok: ('MediaSource' in window) },
                { name: 'WebSocket API', ok: ('WebSocket' in window) },
                { name: 'Canvas 2D', ok: !!document.createElement('canvas').getContext('2d') },
                { name: 'WebGL', ok: !!(document.createElement('canvas').getContext('webgl') || document.createElement('canvas').getContext('experimental-webgl')) }
              ];

              var grid = document.getElementById('capGrid');
              checks.forEach(function(item) {
                var el = document.createElement('div');
                el.className = 'cap';
                el.innerHTML = '<div>' + item.name + '</div>' +
                  '<span class="badge ' + (item.ok ? 'ok' : 'warn') + '">' +
                  (item.ok ? 'SUPPORTED' : 'LIMITED') + '</span>';
                grid.appendChild(el);
              });

              var canvas = document.getElementById('waveCanvas');
              var ctx = canvas.getContext('2d');
              var phase = 0;
              function drawWave() {
                ctx.fillStyle = '#090d14';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                ctx.strokeStyle = '#22d3ee';
                ctx.lineWidth = 3;
                ctx.beginPath();
                for (var x = 0; x < canvas.width; x += 4) {
                  var y = canvas.height / 2 + Math.sin(x * 0.025 + phase) * 28 * Math.cos(x * 0.008);
                  if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                }
                ctx.stroke();
                ctx.strokeStyle = '#10b981';
                ctx.lineWidth = 2;
                ctx.beginPath();
                for (var x2 = 0; x2 < canvas.width; x2 += 4) {
                  var y2 = canvas.height / 2 + Math.cos(x2 * 0.02 - phase * 1.3) * 20;
                  if (x2 === 0) ctx.moveTo(x2, y2); else ctx.lineTo(x2, y2);
                }
                ctx.stroke();
                phase += 0.06;
                requestAnimationFrame(drawWave);
              }
              requestAnimationFrame(drawWave);
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
}
