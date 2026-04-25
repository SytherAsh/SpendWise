"""
SpendWise Test Server
=====================
A bare-bones receiver that catches every POST from the Android app,
pretty-prints the raw JSON, shows what the SMS parser would extract,
and keeps a running count.

Run:   python test.py
Then set your Android app's backend URL to:
    http://YOUR_LAPTOP_IP:8000/api/data
"""

import json
import sys
import os
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Optional

# ---------------------------------------------------------------------------
# Add parent path so we can import the sms_parser from the FastAPI app
# ---------------------------------------------------------------------------
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from app.sms_parser import parse_sms_body
    PARSER_AVAILABLE = True
    print("[OK] sms_parser loaded — will show parsed financial fields too\n")
except ImportError:
    PARSER_AVAILABLE = False
    print("[WARN] Could not import sms_parser — showing raw JSON only\n")


# ---------------------------------------------------------------------------
# Counters and storage
# ---------------------------------------------------------------------------
total_received = 0
sms_count = 0
notif_count = 0
financial_count = 0
all_records = []        # keeps every record in memory for the summary


# ---------------------------------------------------------------------------
# Colors for terminal output (works on Windows 10+ and all Unix terminals)
# ---------------------------------------------------------------------------
os.system("")  # enables ANSI escape codes on Windows

GREEN  = "\033[92m"
YELLOW = "\033[93m"
RED    = "\033[91m"
CYAN   = "\033[96m"
BOLD   = "\033[1m"
DIM    = "\033[2m"
RESET  = "\033[0m"


def print_separator(char="═", width=70):
    print(f"{CYAN}{char * width}{RESET}")


def print_record(data: dict, index: int):
    """Pretty-print a single incoming record with colors."""
    source = data.get("source", "?")
    source_color = YELLOW if source == "sms" else GREEN

    print_separator()
    print(f"  {BOLD}#{index}  {source_color}[{source.upper()}]{RESET}  "
          f"received at {datetime.now().strftime('%H:%M:%S')}")
    print_separator("─")

    # Core fields
    fields = [
        ("id",              data.get("id")),
        ("source",          source),
        ("package_name",    data.get("package_name")),
        ("sender",          data.get("sender")),
        ("title",           data.get("title")),
        ("body",            data.get("body")),
        ("big_text",        data.get("big_text")),
        ("timestamp_ms",    data.get("timestamp_ms")),
        ("timestamp_human", data.get("timestamp_human")),
        ("device_id",       data.get("device_id")),
        ("sent_to_backend", data.get("sent_to_backend")),
    ]

    for name, value in fields:
        val_str = str(value) if value is not None else f"{DIM}null{RESET}"
        # Truncate very long body text for readability
        if name in ("body", "big_text") and value and len(str(value)) > 120:
            val_str = str(value)[:120] + f"{DIM}... ({len(str(value))} chars total){RESET}"
        print(f"  {BOLD}{name:>17}{RESET} : {val_str}")

    # Full body (untruncated) for debugging
    body = data.get("body", "")
    if body and len(body) > 120:
        print(f"\n  {BOLD}{'FULL BODY':>17}{RESET} :")
        # Word-wrap at 80 chars
        for i in range(0, len(body), 80):
            print(f"    {body[i:i+80]}")

    # Show what the parser extracts (if available)
    if PARSER_AVAILABLE:
        full_text = data.get("big_text") or data.get("body", "")
        sender = data.get("sender")
        parsed = parse_sms_body(full_text, sender=sender)

        is_fin = parsed.is_financial
        fin_color = GREEN if is_fin else RED

        print(f"\n  {BOLD}{'── PARSED ──':>17}{RESET}")
        print(f"  {'is_financial':>17} : {fin_color}{is_fin}{RESET}")
        if is_fin:
            print(f"  {'amount':>17} : {parsed.amount}")
            print(f"  {'direction':>17} : {parsed.direction}")
            print(f"  {'bank':>17} : {parsed.bank}")
            print(f"  {'transaction_mode':>17} : {parsed.transaction_mode}")
            print(f"  {'upi_id':>17} : {parsed.upi_id}")
            print(f"  {'recipient_name':>17} : {parsed.recipient_name}")
            print(f"  {'account_suffix':>17} : {parsed.account_suffix}")
            print(f"  {'balance_after':>17} : {parsed.balance_after}")

    print_separator()
    print()


def print_running_stats():
    """Print the current running totals."""
    print(f"  {BOLD}RUNNING TOTALS{RESET}:  "
          f"Total={total_received}  "
          f"SMS={sms_count}  "
          f"Notifications={notif_count}  "
          f"Financial={financial_count}")
    print()

# ---------------------------------------------------------------------------
# HTML helpers
# ---------------------------------------------------------------------------
def _escape(text: str) -> str:
    """Escape HTML special characters to prevent XSS in the dashboard."""
    return (str(text)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace('"', "&quot;"))


def _is_financial_text(record: dict) -> bool:
    """Quick check if a record looks financial based on keywords."""
    body = (record.get("body") or "").lower()
    sender = (record.get("sender") or "").upper()
    fin_keywords = ["debit", "credit", "upi", "imps", "neft", "rtgs",
                    "rs.", "inr", "₹", "transaction", "payment", "withdraw"]
    bank_codes = ["HDFCBK", "SBIINB", "ICICIB", "AXISBK", "PAYTM",
                  "GPAY", "PHONEPE", "KOTAKB", "YESBNK"]
    if any(kw in body for kw in fin_keywords):
        return True
    if any(code in sender for code in bank_codes):
        return True
    return False


# ---------------------------------------------------------------------------
# HTTP Handler
# ---------------------------------------------------------------------------
class SpendWiseTestHandler(BaseHTTPRequestHandler):

    def do_POST(self):
        global total_received, sms_count, notif_count, financial_count

        content_length = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_length)

        try:
            data = json.loads(raw_body)
        except json.JSONDecodeError:
            print(f"\n{RED}[ERROR] Invalid JSON received:{RESET}")
            print(f"  Raw bytes: {raw_body[:500]}")
            self.send_response(400)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"error": "Invalid JSON"}).encode())
            return

        # Update counters
        total_received += 1
        source = data.get("source", "")
        if source == "sms":
            sms_count += 1
        elif source == "notification":
            notif_count += 1

        # Check if financial
        is_financial = False
        if PARSER_AVAILABLE:
            full_text = data.get("big_text") or data.get("body", "")
            parsed = parse_sms_body(full_text, sender=data.get("sender"))
            is_financial = parsed.is_financial
            if is_financial:
                financial_count += 1

        # Store for summary
        all_records.append(data)

        # Print it
        print_record(data, total_received)
        print_running_stats()

        # Respond 200 so the Android app marks it as sent
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        response = json.dumps({
            "status": "ok",
            "raw_id": data.get("id"),
            "is_financial": is_financial,
            "record_number": total_received,
        })
        self.wfile.write(response.encode())

    def do_GET(self):
        """Serve HTML dashboard or JSON depending on path."""
        path = self.path.split("?")[0]

        if path == "/json":
            # Raw JSON endpoint for programmatic access
            self._send_json({
                "total_received": total_received,
                "sms_count": sms_count,
                "notification_count": notif_count,
                "financial_count": financial_count,
                "all_sms": [r for r in all_records if r.get("source") == "sms"],
                "all_notifications": [r for r in all_records if r.get("source") == "notification"],
            })
        elif path == "/sms":
            # JSON list of all SMS only
            self._send_json([r for r in all_records if r.get("source") == "sms"])
        elif path == "/notifications":
            # JSON list of all notifications only
            self._send_json([r for r in all_records if r.get("source") == "notification"])
        else:
            # HTML dashboard
            self._send_html()

    def _send_json(self, data):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(data, indent=2, ensure_ascii=False).encode("utf-8"))

    def _send_html(self):
        sms_list = [r for r in all_records if r.get("source") == "sms"]
        notif_list = [r for r in all_records if r.get("source") == "notification"]

        # Build SMS table rows
        sms_rows = ""
        for i, r in enumerate(sms_list, 1):
            body = (r.get("body") or "")
            is_fin = "✅" if _is_financial_text(r) else "❌"
            sms_rows += f"""<tr>
                <td>{i}</td>
                <td>{r.get('sender') or '—'}</td>
                <td class="body-cell">{_escape(body)}</td>
                <td>{r.get('timestamp_human') or '—'}</td>
                <td>{is_fin}</td>
            </tr>"""

        # Build notification table rows
        notif_rows = ""
        for i, r in enumerate(notif_list, 1):
            body = (r.get("body") or "")
            pkg = (r.get("package_name") or "—")
            # Shorten package name for readability
            short_pkg = pkg.split(".")[-1] if "." in pkg else pkg
            notif_rows += f"""<tr>
                <td>{i}</td>
                <td title="{_escape(pkg)}">{_escape(short_pkg)}</td>
                <td>{_escape(r.get('title') or '—')}</td>
                <td class="body-cell">{_escape(body)}</td>
                <td>{r.get('timestamp_human') or '—'}</td>
            </tr>"""

        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="refresh" content="5">
    <title>SpendWise — Live Dashboard</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
            background: #0f0f1a;
            color: #e0e0e0;
            padding: 20px;
        }}
        h1 {{
            text-align: center;
            font-size: 28px;
            margin-bottom: 10px;
            background: linear-gradient(90deg, #448aff, #b388ff);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }}
        .subtitle {{ text-align: center; color: #9e9e9e; margin-bottom: 24px; font-size: 13px; }}
        .stats {{
            display: flex;
            gap: 16px;
            justify-content: center;
            margin-bottom: 30px;
            flex-wrap: wrap;
        }}
        .stat-card {{
            background: #1a1a2e;
            border-radius: 14px;
            padding: 18px 28px;
            text-align: center;
            min-width: 140px;
        }}
        .stat-card .number {{
            font-size: 32px;
            font-weight: 700;
        }}
        .stat-card .label {{
            font-size: 12px;
            color: #9e9e9e;
            margin-top: 4px;
        }}
        .total .number {{ color: #448aff; }}
        .sms .number {{ color: #ffd740; }}
        .notif .number {{ color: #69f0ae; }}
        .fin .number {{ color: #ff5252; }}
        h2 {{
            font-size: 18px;
            margin: 30px 0 12px 0;
            padding-left: 4px;
            border-left: 3px solid #448aff;
            padding-left: 10px;
        }}
        h2.sms-h {{ border-color: #ffd740; }}
        h2.notif-h {{ border-color: #69f0ae; }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
            font-size: 13px;
        }}
        th {{
            background: #1a1a2e;
            padding: 10px 12px;
            text-align: left;
            font-weight: 600;
            color: #b0b0b0;
            position: sticky;
            top: 0;
        }}
        td {{
            padding: 8px 12px;
            border-bottom: 1px solid #1e1e3a;
            vertical-align: top;
        }}
        tr:hover {{ background: #1a1a2e; }}
        .body-cell {{
            max-width: 500px;
            word-wrap: break-word;
            white-space: pre-wrap;
            font-size: 12px;
            color: #ccc;
        }}
        .empty {{ color: #666; text-align: center; padding: 40px; }}
        .tab-bar {{
            display: flex;
            gap: 0;
            margin-bottom: 0;
        }}
        .tab {{
            padding: 10px 24px;
            background: #1a1a2e;
            cursor: pointer;
            border-radius: 10px 10px 0 0;
            font-weight: 600;
            font-size: 14px;
            color: #9e9e9e;
        }}
        .tab.active {{ background: #252545; color: #fff; }}
        .tab-panel {{ display: none; background: #252545; border-radius: 0 10px 10px 10px; padding: 16px; }}
        .tab-panel.active {{ display: block; }}
    </style>
</head>
<body>
    <h1>📱 SpendWise Live Dashboard</h1>
    <p class="subtitle">Auto-refreshes every 5 seconds · {total_received} records captured</p>

    <div class="stats">
        <div class="stat-card total">
            <div class="number">{total_received}</div>
            <div class="label">Total</div>
        </div>
        <div class="stat-card sms">
            <div class="number">{sms_count}</div>
            <div class="label">SMS</div>
        </div>
        <div class="stat-card notif">
            <div class="number">{notif_count}</div>
            <div class="label">Notifications</div>
        </div>
        <div class="stat-card fin">
            <div class="number">{financial_count}</div>
            <div class="label">Financial</div>
        </div>
    </div>

    <div class="tab-bar">
        <div class="tab active" onclick="showTab('sms')">📨 SMS ({sms_count})</div>
        <div class="tab" onclick="showTab('notif')">🔔 Notifications ({notif_count})</div>
    </div>

    <div id="sms-panel" class="tab-panel active">
        {f'''<table>
            <thead><tr>
                <th>#</th><th>Sender</th><th>Body</th><th>Time</th><th>Financial</th>
            </tr></thead>
            <tbody>{sms_rows}</tbody>
        </table>''' if sms_list else '<p class="empty">No SMS received yet. Open the SpendWise app and tap "Retry Unsent Records".</p>'}
    </div>

    <div id="notif-panel" class="tab-panel">
        {f'''<table>
            <thead><tr>
                <th>#</th><th>App</th><th>Title</th><th>Body</th><th>Time</th>
            </tr></thead>
            <tbody>{notif_rows}</tbody>
        </table>''' if notif_list else '<p class="empty">No notifications received yet. Notifications are captured in real-time after you enable access.</p>'}
    </div>

    <script>
        function showTab(name) {{
            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            if (name === 'sms') {{
                document.getElementById('sms-panel').classList.add('active');
                document.querySelectorAll('.tab')[0].classList.add('active');
            }} else {{
                document.getElementById('notif-panel').classList.add('active');
                document.querySelectorAll('.tab')[1].classList.add('active');
            }}
        }}
    </script>
</body>
</html>"""
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(html.encode("utf-8"))

    def log_message(self, format, *args):
        """Suppress default HTTP logging — we do our own pretty printing."""
        pass


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    port = 8000
    if len(sys.argv) > 1:
        port = int(sys.argv[1])

    server = HTTPServer(("0.0.0.0", port), SpendWiseTestHandler)

    print(f"{BOLD}{GREEN}")
    print(f"  ╔══════════════════════════════════════════════════╗")
    print(f"  ║       SpendWise Test Server — RUNNING           ║")
    print(f"  ╚══════════════════════════════════════════════════╝{RESET}")
    print()
    print(f"  Listening on {BOLD}http://0.0.0.0:{port}{RESET}")
    print(f"  Android app should POST to: {BOLD}http://YOUR_LAPTOP_IP:{port}/api/data{RESET}")
    print()
    print(f"  {DIM}• Every incoming record will be printed below")
    print(f"  • GET http://localhost:{port}/ shows a JSON summary")
    print(f"  • Press Ctrl+C to stop and see final summary{RESET}")
    print()
    print_separator()
    print(f"  Waiting for data from SpendWise app...")
    print_separator()
    print()

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print(f"\n\n{BOLD}{YELLOW}Server stopped. Final summary:{RESET}\n")
        print_separator()
        print(f"  Total records received : {BOLD}{total_received}{RESET}")
        print(f"  SMS records            : {sms_count}")
        print(f"  Notification records   : {notif_count}")
        print(f"  Financial records      : {financial_count}")
        print(f"  Non-financial records  : {total_received - financial_count}")
        print_separator()

        # Show unique senders
        senders = set()
        packages = set()
        for r in all_records:
            if r.get("sender"):
                senders.add(r["sender"])
            if r.get("package_name"):
                packages.add(r["package_name"])

        if senders:
            print(f"\n  {BOLD}Unique SMS senders ({len(senders)}):{RESET}")
            for s in sorted(senders):
                count = sum(1 for r in all_records if r.get("sender") == s)
                print(f"    {s:30s}  ({count} messages)")

        if packages:
            print(f"\n  {BOLD}Unique notification apps ({len(packages)}):{RESET}")
            for p in sorted(packages):
                count = sum(1 for r in all_records if r.get("package_name") == p)
                print(f"    {p:40s}  ({count} notifications)")

        # Dump all records to a JSON file for later inspection
        dump_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "test_dump.json")
        try:
            with open(dump_path, "w", encoding="utf-8") as f:
                json.dump(all_records, f, indent=2, ensure_ascii=False)
            print(f"\n  {GREEN}All records saved to: {dump_path}{RESET}")
        except Exception as e:
            print(f"\n  {RED}Could not save dump: {e}{RESET}")

        print()
        server.server_close()


if __name__ == "__main__":
    main()
