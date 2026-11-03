package com.gofoody.chatbot;

/**
 * Chat Widget (frontend) — nút nổi + popup chat ở góc dưới phải.
 *
 * Trả về một chuỗi HTML chứa CSS + JS inline (tự chứa, KHÔNG cần file static mới).
 * Được chèn vào layout bằng 1 dòng duy nhất (xem Html.layout).
 *
 * Tương tác với backend qua: POST /api/chatbot/ask (JSON).
 *
 * Lưu ý về escape trong Java text block:
 *  - Muốn JS có dấu xuống dòng '\n'  => ghi '\\n'  (2 backslash) trong source.
 *  - Muốn JS có '\u2022' (bullet)    => ghi '\\u2022'.
 *  - Muốn JS có '\u20b9' (₹)        => ghi '\\u20b9'.
 *  - KHÔNG bao giờ dùng '\' đơn trong comment JS (text block sẽ thành newline thật).
 */
public final class ChatbotWidget {

    private ChatbotWidget() {}

    public static String widget() {
        return """
            <!-- ===== GOFOODY AI CHATBOT WIDGET (file mới, KHÔNG sửa gì khác) ===== -->
            <style>
              #gf-chat-fab {
                position: fixed; right: 22px; bottom: 22px; z-index: 9999;
                width: 62px; height: 62px; border-radius: 50%; border: none; cursor: pointer;
                background: linear-gradient(135deg,#ff6b00,#ffbf00);
                color: #fff; font-size: 26px; line-height: 62px; text-align: center;
                box-shadow: 0 10px 26px rgba(255,107,0,.45);
                transition: transform .15s ease, box-shadow .15s ease;
              }
              #gf-chat-fab:hover { transform: translateY(-3px) scale(1.05); }
              #gf-chat-panel {
                position: fixed; right: 22px; bottom: 96px; z-index: 9999;
                width: min(380px, 92vw); height: min(560px, 78vh);
                background: #fff; border-radius: 18px; overflow: hidden;
                box-shadow: 0 18px 50px rgba(0,0,0,.28);
                display: none; flex-direction: column;
                border: 1px solid #f0e6db;
                font-family: Arial, Helvetica, sans-serif;
              }
              #gf-chat-panel.open { display: flex; animation: gfpop .18s ease; }
              @keyframes gfpop { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: none; } }
              #gf-chat-head {
                background: #111; color: #fff; padding: 14px 16px; display: flex;
                align-items: center; justify-content: space-between;
              }
              #gf-chat-head .gf-title { font-weight: 800; font-size: 15px; }
              #gf-chat-head .gf-title small { display:block; font-weight:400; color:#ffbf00; font-size:11px; }
              #gf-chat-close { background:none; border:none; color:#fff; font-size:20px; cursor:pointer; line-height:1; }
              #gf-chat-msgs { flex: 1; overflow-y: auto; padding: 14px; background: #fff8f0; box-sizing: border-box; }
              .gf-msg { max-width: 84%; margin: 0 0 12px; padding: 10px 13px; border-radius: 14px; font-size: 13.5px; line-height: 1.5; word-wrap: break-word; box-sizing: border-box; }
              .gf-msg.bot { background: #fff; border: 1px solid #f0e6db; border-bottom-left-radius: 4px; }
              .gf-msg.user { background: #ff6b00; color: #fff; margin-left: auto; border-bottom-right-radius: 4px; }
              .gf-msg strong { font-weight: 800; }
              .gf-prods { display:flex; flex-direction:column; gap:8px; margin-top:10px; box-sizing: border-box; }
              .gf-prod {
                display:flex; gap:10px; align-items:center; text-decoration:none; color:#27221f;
                background:#fff8f0; border:1px solid #ffe0b8; border-radius:12px; padding:8px; box-sizing: border-box;
              }
              .gf-prod:hover { border-color:#ff6b00; }
              .gf-prod img { width:46px; height:46px; border-radius:10px; object-fit:cover; background:#eee; flex:0 0 auto; }
              .gf-prod .gf-pn { font-weight:800; font-size:13px; }
              .gf-prod .gf-pd { font-size:11.5px; color:#7a6f64; }
              .gf-prod .gf-pp { font-weight:800; color:#ff6b00; font-size:12.5px; }
              .gf-typ { color:#999; font-style:italic; font-size:13px; }
              #gf-chat-form { display:flex; gap:8px; padding:10px; border-top:1px solid #eee; background:#fff; box-sizing: border-box; }
              #gf-chat-input {
                flex:1; border:1px solid #ddd; border-radius:12px; padding:11px 12px;
                font-size:13.5px; outline:none; box-sizing: border-box;
              }
              #gf-chat-input:focus { border-color:#ff6b00; }
              #gf-chat-send {
                background:#ff6b00; color:#fff; border:none; border-radius:12px;
                padding:0 16px; font-weight:800; cursor:pointer; font-size:14px;
              }
              #gf-chat-send:hover { background:#d95600; }
              #gf-chat-send:disabled { background:#ccc; cursor:not-allowed; }
              @media (max-width: 760px) { #gf-chat-fab { right: 14px; bottom: 14px; } #gf-chat-panel { right: 4vw; } }
            </style>

            <button id="gf-chat-fab" title="Hỏi GoFoody Assistant" aria-label="Mở chat tư vấn AI">💬</button>
            <div id="gf-chat-panel" role="dialog" aria-label="GoFoody AI Chat">
              <div id="gf-chat-head">
                <div class="gf-title">GoFoody Assistant<small>Tư vấn món ăn bằng AI</small></div>
                <button id="gf-chat-close" aria-label="Đóng chat">&times;</button>
              </div>
              <div id="gf-chat-msgs"></div>
              <form id="gf-chat-form" autocomplete="off">
                <input id="gf-chat-input" type="text" placeholder="VD: Có món gà gì không?" aria-label="Nhập câu hỏi">
                <button id="gf-chat-send" type="submit">Gửi</button>
              </form>
            </div>

            <script>
            (function () {
              var fab = document.getElementById('gf-chat-fab');
              var panel = document.getElementById('gf-chat-panel');
              var closeBtn = document.getElementById('gf-chat-close');
              var msgs = document.getElementById('gf-chat-msgs');
              var form = document.getElementById('gf-chat-form');
              var input = document.getElementById('gf-chat-input');
              var sendBtn = document.getElementById('gf-chat-send');

              function toggle() {
                panel.classList.toggle('open');
                if (panel.classList.contains('open')) { input.focus(); }
              }
              fab.addEventListener('click', toggle);
              closeBtn.addEventListener('click', function () { panel.classList.remove('open'); });

              function esc(s) {
                return String(s == null ? '' : s)
                  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                  .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
              }
              // in đậm **x** (không dùng regex có backslash)
              function inline(s) {
                var p = String(s).split('**');
                for (var i = 1; i < p.length; i += 2) { p[i] = '<strong>' + p[i] + '</strong>'; }
                return p.join('');
              }
              // tách câu trả lời theo dòng (split theo ký tự xuống dòng)
              function fmt(text) {
                if (!text) return '';
                var lines = String(text).split('\\n');
                var html = '';
                for (var i = 0; i < lines.length; i++) {
                  var ln = lines[i];
                  if (ln.trim() === '') continue;
                  if (ln.charAt(0) === '\\u2022') {
                    html += '<div style="margin:2px 0">' + inline(esc(ln)) + '</div>';
                  } else {
                    html += '<p style="margin:0 0 6px">' + inline(esc(ln)) + '</p>';
                  }
                }
                return html;
              }
              function addMsg(role, html) {
                var d = document.createElement('div');
                d.className = 'gf-msg ' + role;
                d.innerHTML = html;
                msgs.appendChild(d);
                msgs.scrollTop = msgs.scrollHeight;
                return d;
              }
              function addProducts(list) {
                if (!list || !list.length) return;
                var box = document.createElement('div');
                box.className = 'gf-prods';
                for (var i = 0; i < list.length; i++) {
                  var p = list[i];
                  var a = document.createElement('a');
                  a.className = 'gf-prod';
                  a.href = p.link || '#';
                  var img = p.image || '/static/noimage.png';
                  a.innerHTML = '<img src="' + esc(img) + '" onerror="this.src=&quot;/static/noimage.png&quot;">'
                    + '<div><div class="gf-pn">' + esc(p.name) + '</div>'
                    + '<div class="gf-pd">' + esc(p.restaurant || '') + '</div>'
                    + '<div class="gf-pp">\\u20b9' + esc(p.price) + '</div></div>';
                  box.appendChild(a);
                }
                msgs.appendChild(box);
                msgs.scrollTop = msgs.scrollHeight;
              }

              addMsg('bot', 'Chào bạn 👋 Mình là <strong>GoFoody Assistant</strong>. Hỏi mình về món ăn, giá, nhà hàng nhé!');

              function send() {
                var q = input.value.trim();
                if (!q) return;
                addMsg('user', esc(q));
                input.value = '';
                sendBtn.disabled = true;
                var typing = addMsg('bot', '<span class="gf-typ">Đang tìm món phù hợp...</span>');
                fetch('/api/chatbot/ask', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ question: q })
                }).then(function (r) { return r.json(); })
                  .then(function (data) {
                    typing.remove();
                    addMsg('bot', fmt(data.answer));
                    addProducts(data.products);
                  }).catch(function () {
                    typing.remove();
                    addMsg('bot', '⚠️ Không kết nối được tới máy chủ. Vui lòng thử lại.');
                  }).finally(function () { sendBtn.disabled = false; input.focus(); });
              }

              form.addEventListener('submit', function (e) { e.preventDefault(); send(); });
            })();
            </script>
            <!-- ===== END GOFOODY AI CHATBOT WIDGET ===== -->
            """;
    }
}
