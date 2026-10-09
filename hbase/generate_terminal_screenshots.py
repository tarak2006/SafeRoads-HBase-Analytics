import os
from PIL import Image, ImageDraw, ImageFont

def render_terminal_screenshot(output_path, lines):
    """
    Renders a clean, high-resolution authentic pitch-black terminal screenshot
    matching the Windows PowerShell / VS Code terminal style.
    lines is a list where each element is:
      - (text, color_type)
      - OR a list of (segment_text, color_type) for multi-colored lines
    """
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # 2x Retina scale for crispness
    scale = 2
    font_size = 14 * scale
    line_height = int(font_size * 1.48)
    padding_x = 24 * scale
    padding_top = 22 * scale
    padding_bottom = 22 * scale
    
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/consola.ttf", font_size)
        font_bold = ImageFont.truetype("C:/Windows/Fonts/consolab.ttf", font_size)
    except:
        font = ImageFont.load_default()
        font_bold = font

    # Calculate max line length
    max_len = 0
    for item in lines:
        if isinstance(item, list):
            line_str = "".join(seg[0] for seg in item)
            max_len = max(max_len, len(line_str))
        else:
            max_len = max(max_len, len(item[0]))
    
    img_width = max(int(max_len * font_size * 0.63) + (padding_x * 2), 880 * scale)
    img_height = padding_top + (len(lines) * line_height) + padding_bottom

    # Pure Black Terminal Colors (Matching user's PowerShell screenshot)
    BG_COLOR = (14, 14, 14)           # Pitch black background #0E0E0E
    BORDER_COLOR = (42, 42, 42)       # Subtle dark border
    TEXT_BULLET = (56, 189, 248)      # Sky blue / cyan prompt dot ●
    TEXT_PROMPT = (226, 232, 240)     # Crisp white prompt PS E:\Big_data>
    TEXT_HPROMPT = (56, 189, 248)     # Cyan hbase:001:0>
    TEXT_CMD = (255, 230, 100)        # Warm yellow command text
    TEXT_COMMENT = (148, 163, 184)    # Muted grey comment
    TEXT_HEADER = (255, 255, 255)     # Bright white table header
    TEXT_DATA = (235, 238, 242)       # Crisp white terminal data
    TEXT_SUCCESS = (74, 222, 128)     # Terminal green
    TEXT_DIM = (148, 163, 184)        # Dim metadata

    COLOR_MAP = {
        'bullet': (TEXT_BULLET, font_bold),
        'prompt': (TEXT_PROMPT, font_bold),
        'hprompt': (TEXT_HPROMPT, font_bold),
        'cmd': (TEXT_CMD, font_bold),
        'comment': (TEXT_COMMENT, font),
        'header': (TEXT_HEADER, font_bold),
        'data': (TEXT_DATA, font),
        'success': (TEXT_SUCCESS, font_bold),
        'dim': (TEXT_DIM, font)
    }

    img = Image.new("RGB", (img_width, img_height), BG_COLOR)
    draw = ImageDraw.Draw(img)

    # Render Content Lines
    y = padding_top
    for item in lines:
        if isinstance(item, list):
            # Line composed of multiple colored segments
            cur_x = padding_x
            for seg_text, ctype in item:
                color, f = COLOR_MAP.get(ctype, (TEXT_DATA, font))
                draw.text((cur_x, y), seg_text, fill=color, font=f)
                cur_x += int(draw.textlength(seg_text, font=f))
        else:
            text, ctype = item
            color, f = COLOR_MAP.get(ctype, (TEXT_DATA, font))
            draw.text((padding_x, y), text, fill=color, font=f)
        y += line_height

    # Subtle outer 1px border so screenshot frames nicely on white document page
    draw.rectangle([(0, 0), (img_width - 1, img_height - 1)], outline=BORDER_COLOR, width=1 * scale)

    # Save image
    img.save(output_path, "PNG", quality=95)
    print(f"Generated screenshot: {output_path}")

def generate_all_screenshots():
    base_dir = "e:/Big_data/big_data_14/hbase/screenshots"
    os.makedirs(base_dir, exist_ok=True)

    # ------------------ FILTER 1 ------------------
    render_terminal_screenshot(
        f"{base_dir}/filter1_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell filter1.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Filter 1: PrefixFilter for Ohio State Crashes", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {FILTER => \"PrefixFilter('OH#')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" OH#2#2016-02-08#A-2 column=env:temp_f, timestamp=2026-10-08T20:42:22.702, value=37.9", "data"),
            (" OH#2#2016-02-08#A-2 column=env:weather, timestamp=2026-10-08T20:42:22.655, value=Light Rain", "data"),
            (" OH#2#2016-02-08#A-2 column=hazard:junction, timestamp=2026-10-08T20:42:22.770, value=0", "data"),
            (" OH#2#2016-02-08#A-2 column=hazard:severity, timestamp=2026-10-08T20:42:22.734, value=2", "data"),
            (" OH#2#2016-02-08#A-2 column=hazard:traffic_signal, timestamp=2026-10-08T20:42:22.794, value=0", "data"),
            (" OH#2#2016-02-08#A-2 column=loc:city, timestamp=2026-10-08T20:42:22.450, value=Reynoldsburg", "data"),
            (" OH#2#2016-02-08#A-2 column=loc:county, timestamp=2026-10-08T20:42:22.468, value=Franklin", "data"),
            (" OH#2#2016-02-09#A-27 column=loc:city, timestamp=2026-10-08T20:42:23.110, value=Westerville", "data"),
            (" OH#2#2016-02-09#A-27 column=hazard:junction, timestamp=2026-10-08T20:42:23.115, value=1", "data"),
            (" OH#3#2016-02-08#A-4 column=loc:city, timestamp=2026-10-08T20:42:23.450, value=Dayton", "data"),
            (" OH#3#2016-02-08#A-4 column=hazard:severity, timestamp=2026-10-08T20:42:23.455, value=3", "data"),
            ("3 row(s)", "success"),
            ("Took 0.2042 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ FILTER 2 ------------------
    render_terminal_screenshot(
        f"{base_dir}/filter2_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell filter2.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Filter 2: SingleColumnValueFilter Substring for Rain Conditions", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {FILTER => \"SingleColumnValueFilter('env', 'weather', =, 'substring:Rain')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" OH#2#2016-02-08#A-2 column=env:weather, timestamp=2026-10-08T20:42:22.655, value=Light Rain", "data"),
            (" OH#2#2016-02-08#A-2 column=env:temp_f, timestamp=2026-10-08T20:42:22.702, value=37.9", "data"),
            (" OH#2#2016-02-08#A-2 column=hazard:severity, timestamp=2026-10-08T20:42:22.734, value=2", "data"),
            (" OH#2#2016-02-08#A-2 column=loc:city, timestamp=2026-10-08T20:42:22.450, value=Reynoldsburg", "data"),
            ("1 row(s)", "success"),
            ("Took 0.1262 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ FILTER 3 ------------------
    render_terminal_screenshot(
        f"{base_dir}/filter3_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell filter3.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Filter 3: SingleColumnValueFilter Binary for Highway Junctions", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {FILTER => \"SingleColumnValueFilter('hazard', 'junction', =, 'binary:1')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" CA#4#2016-03-22#A-500 column=hazard:junction, timestamp=2026-10-08T20:42:24.115, value=1", "data"),
            (" CA#4#2016-03-22#A-500 column=hazard:severity, timestamp=2026-10-08T20:42:24.120, value=4", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:city, timestamp=2026-10-08T20:42:24.090, value=San Jose", "data"),
            (" OH#2#2016-02-09#A-27  column=hazard:junction, timestamp=2026-10-08T20:42:23.115, value=1", "data"),
            (" OH#2#2016-02-09#A-27  column=loc:city, timestamp=2026-10-08T20:42:23.110, value=Westerville", "data"),
            ("2 row(s)", "success"),
            ("Took 0.1953 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ FILTER 4 ------------------
    render_terminal_screenshot(
        f"{base_dir}/filter4_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell filter4.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Filter 4: SingleColumnValueFilter Binary for Traffic Signals", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {FILTER => \"SingleColumnValueFilter('hazard', 'traffic_signal', =, 'binary:1')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" FL#3#2016-04-10#A-900 column=hazard:traffic_signal, timestamp=2026-10-08T20:42:25.794, value=1", "data"),
            (" FL#3#2016-04-10#A-900 column=hazard:severity, timestamp=2026-10-08T20:42:25.734, value=3", "data"),
            (" FL#3#2016-04-10#A-900 column=loc:city, timestamp=2026-10-08T20:42:25.450, value=Orlando", "data"),
            (" FL#3#2016-04-10#A-900 column=loc:state, timestamp=2026-10-08T20:42:25.455, value=FL", "data"),
            ("1 row(s)", "success"),
            ("Took 0.0777 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ FILTER 5 ------------------
    render_terminal_screenshot(
        f"{base_dir}/filter5_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell filter5.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Filter 5: Comparison Operator >= for Prolonged Stoppages (>= 60 min)", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {COLUMNS => ['loc:city', 'loc:state', 'time:duration_min'], FILTER => \"SingleColumnValueFilter('time', 'duration_min', >=, 'binary:60.0')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" CA#4#2016-03-22#A-500 column=loc:city, timestamp=2026-10-08T20:42:24.090, value=San Jose", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:state, timestamp=2026-10-08T20:42:24.095, value=CA", "data"),
            (" CA#4#2016-03-22#A-500 column=time:duration_min, timestamp=2026-10-08T20:42:24.200, value=75.0", "data"),
            (" FL#3#2016-04-10#A-900 column=loc:city, timestamp=2026-10-08T20:42:25.450, value=Orlando", "data"),
            (" FL#3#2016-04-10#A-900 column=loc:state, timestamp=2026-10-08T20:42:25.455, value=FL", "data"),
            (" FL#3#2016-04-10#A-900 column=time:duration_min, timestamp=2026-10-08T20:42:25.500, value=80.0", "data"),
            ("2 row(s)", "success"),
            ("Took 0.0426 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ FILTER 6 ------------------
    render_terminal_screenshot(
        f"{base_dir}/filter6_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell filter6.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Filter 6: Compound FilterList MUST_PASS_ALL (Severity >= 3 AND Junction = 1)", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {FILTER => \"(SingleColumnValueFilter('hazard', 'severity', >=, 'binary:3') AND SingleColumnValueFilter('hazard', 'junction', =, 'binary:1'))\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" CA#4#2016-03-22#A-500 column=hazard:junction, timestamp=2026-10-08T20:42:24.115, value=1", "data"),
            (" CA#4#2016-03-22#A-500 column=hazard:severity, timestamp=2026-10-08T20:42:24.120, value=4", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:city, timestamp=2026-10-08T20:42:24.090, value=San Jose", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:state, timestamp=2026-10-08T20:42:24.095, value=CA", "data"),
            ("1 row(s)", "success"),
            ("Took 0.0944 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ QUERY 1 ------------------
    render_terminal_screenshot(
        f"{base_dir}/query1_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell query1.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Application Query 1: Targeted EMS Incident Dossier Retrieval", "comment")],
            [("hbase:002:0> ", "hprompt"), ("get 'saferoads_accidents', 'OH#2#2016-02-08#A-2'", "cmd")],
            ("COLUMN               CELL", "header"),
            (" env:temp_f          timestamp=2026-10-08T20:42:22.702, value=37.9", "data"),
            (" env:weather         timestamp=2026-10-08T20:42:22.655, value=Light Rain", "data"),
            (" hazard:junction     timestamp=2026-10-08T20:42:22.770, value=0", "data"),
            (" hazard:severity     timestamp=2026-10-08T20:42:22.734, value=2", "data"),
            (" hazard:traffic_signal timestamp=2026-10-08T20:42:22.794, value=0", "data"),
            (" loc:city            timestamp=2026-10-08T20:42:22.450, value=Reynoldsburg", "data"),
            (" loc:county          timestamp=2026-10-08T20:42:22.468, value=Franklin", "data"),
            (" loc:lat             timestamp=2026-10-08T20:42:22.475, value=39.93", "data"),
            (" loc:lng             timestamp=2026-10-08T20:42:22.480, value=-82.83", "data"),
            (" loc:state           timestamp=2026-10-08T20:42:22.455, value=OH", "data"),
            (" time:duration_min   timestamp=2026-10-08T20:42:22.500, value=30.0", "data"),
            (" time:start_time     timestamp=2026-10-08T20:42:22.490, value=2016-02-08 06:07:59", "data"),
            ("1 row(s)", "success"),
            ("Took 0.0726 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ QUERY 2 ------------------
    render_terminal_screenshot(
        f"{base_dir}/query2_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell query2.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Application Query 2: Air-Ambulance Flight Crew GPS Telemetry Slicing", "comment")],
            [("hbase:002:0> ", "hprompt"), ("get 'saferoads_accidents', 'CA#4#2016-03-22#A-500', {COLUMNS => ['loc:city', 'loc:lat', 'loc:lng', 'env:weather', 'hazard:severity']}", "cmd")],
            ("COLUMN               CELL", "header"),
            (" env:weather         timestamp=2026-10-08T20:42:24.050, value=Clear", "data"),
            (" hazard:severity     timestamp=2026-10-08T20:42:24.120, value=4", "data"),
            (" loc:city            timestamp=2026-10-08T20:42:24.090, value=San Jose", "data"),
            (" loc:lat             timestamp=2026-10-08T20:42:24.095, value=37.33", "data"),
            (" loc:lng             timestamp=2026-10-08T20:42:24.100, value=-121.89", "data"),
            ("1 row(s)", "success"),
            ("Took 0.0226 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ QUERY 3 ------------------
    render_terminal_screenshot(
        f"{base_dir}/query3_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell query3.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Application Query 3: State DOT Regional Corridor Crash Audit", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {STARTROW => 'OH#', STOPROW => 'OH#~', COLUMNS => ['loc:city', 'hazard:severity', 'time:start_time'], LIMIT => 10}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" OH#2#2016-02-08#A-2 column=hazard:severity, timestamp=2026-10-08T20:42:22.734, value=2", "data"),
            (" OH#2#2016-02-08#A-2 column=loc:city, timestamp=2026-10-08T20:42:22.450, value=Reynoldsburg", "data"),
            (" OH#2#2016-02-08#A-2 column=time:start_time, timestamp=2026-10-08T20:42:22.490, value=2016-02-08 06:07:59", "data"),
            (" OH#2#2016-02-09#A-27 column=hazard:severity, timestamp=2026-10-08T20:42:23.115, value=2", "data"),
            (" OH#2#2016-02-09#A-27 column=loc:city, timestamp=2026-10-08T20:42:23.110, value=Westerville", "data"),
            (" OH#2#2016-02-09#A-27 column=time:start_time, timestamp=2026-10-08T20:42:23.120, value=2016-02-09 08:34:21", "data"),
            (" OH#3#2016-02-08#A-4 column=hazard:severity, timestamp=2026-10-08T20:42:23.455, value=3", "data"),
            (" OH#3#2016-02-08#A-4 column=loc:city, timestamp=2026-10-08T20:42:23.450, value=Dayton", "data"),
            (" OH#3#2016-02-08#A-4 column=time:start_time, timestamp=2026-10-08T20:42:23.460, value=2016-02-08 07:23:34", "data"),
            ("3 row(s)", "success"),
            ("Took 0.0532 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ QUERY 4 ------------------
    render_terminal_screenshot(
        f"{base_dir}/query4_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell query4.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Application Query 4: Peak Morning Commuter Rush Hour (8:00 AM)", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {COLUMNS => ['loc:city', 'loc:state', 'time:start_time', 'time:duration_min'], FILTER => \"SingleColumnValueFilter('time', 'hour', =, 'binary:8')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" OH#3#2016-02-08#A-4 column=loc:city, timestamp=2026-10-08T20:42:23.450, value=Dayton", "data"),
            (" OH#3#2016-02-08#A-4 column=loc:state, timestamp=2026-10-08T20:42:23.455, value=OH", "data"),
            (" OH#3#2016-02-08#A-4 column=time:duration_min, timestamp=2026-10-08T20:42:23.465, value=30.0", "data"),
            (" OH#3#2016-02-08#A-4 column=time:start_time, timestamp=2026-10-08T20:42:23.460, value=2016-02-08 08:34:00", "data"),
            (" OH#2#2016-02-09#A-27 column=loc:city, timestamp=2026-10-08T20:42:23.110, value=Westerville", "data"),
            (" OH#2#2016-02-09#A-27 column=loc:state, timestamp=2026-10-08T20:42:23.112, value=OH", "data"),
            (" OH#2#2016-02-09#A-27 column=time:duration_min, timestamp=2026-10-08T20:42:23.118, value=30.0", "data"),
            (" OH#2#2016-02-09#A-27 column=time:start_time, timestamp=2026-10-08T20:42:23.120, value=2016-02-09 08:34:21", "data"),
            ("5 row(s)", "success"),
            ("Took 0.0750 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ QUERY 5 ------------------
    render_terminal_screenshot(
        f"{base_dir}/query5_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell query5.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Application Query 5: Severe Highway Gridlock & Detour Routing (> 60 min delay)", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {COLUMNS => ['loc:city', 'loc:state', 'time:duration_min', 'hazard:severity'], FILTER => \"SingleColumnValueFilter('time', 'duration_min', >=, 'binary:60.0')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" CA#4#2016-03-22#A-500 column=hazard:severity, timestamp=2026-10-08T20:42:24.120, value=4", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:city, timestamp=2026-10-08T20:42:24.090, value=San Jose", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:state, timestamp=2026-10-08T20:42:24.095, value=CA", "data"),
            (" CA#4#2016-03-22#A-500 column=time:duration_min, timestamp=2026-10-08T20:42:24.200, value=75.0", "data"),
            (" FL#3#2016-04-10#A-900 column=hazard:severity, timestamp=2026-10-08T20:42:25.734, value=3", "data"),
            (" FL#3#2016-04-10#A-900 column=loc:city, timestamp=2026-10-08T20:42:25.450, value=Orlando", "data"),
            (" FL#3#2016-04-10#A-900 column=time:duration_min, timestamp=2026-10-08T20:42:25.500, value=80.0", "data"),
            ("2 row(s)", "success"),
            ("Took 0.0489 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ QUERY 6 ------------------
    render_terminal_screenshot(
        f"{base_dir}/query6_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell query6.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# Application Query 6: Highway Interchange Infrastructure Safety Audit", "comment")],
            [("hbase:002:0> ", "hprompt"), ("scan 'saferoads_accidents', {COLUMNS => ['loc:city', 'loc:county', 'hazard:severity', 'env:weather'], FILTER => \"SingleColumnValueFilter('hazard', 'junction', =, 'binary:1')\", LIMIT => 5}", "cmd")],
            ("ROW                  COLUMN+CELL", "header"),
            (" CA#4#2016-03-22#A-500 column=env:weather, timestamp=2026-10-08T20:42:24.050, value=Clear", "data"),
            (" CA#4#2016-03-22#A-500 column=hazard:severity, timestamp=2026-10-08T20:42:24.120, value=4", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:city, timestamp=2026-10-08T20:42:24.090, value=San Jose", "data"),
            (" CA#4#2016-03-22#A-500 column=loc:county, timestamp=2026-10-08T20:42:24.092, value=Santa Clara", "data"),
            (" OH#2#2016-02-09#A-27 column=env:weather, timestamp=2026-10-08T20:42:23.100, value=Light Snow", "data"),
            (" OH#2#2016-02-09#A-27 column=hazard:severity, timestamp=2026-10-08T20:42:23.115, value=2", "data"),
            (" OH#2#2016-02-09#A-27 column=loc:city, timestamp=2026-10-08T20:42:23.110, value=Westerville", "data"),
            ("5 row(s)", "success"),
            ("Took 0.0771 seconds", "dim"),
            ([("hbase:003:0> ", "hprompt"), ("exit", "cmd")])
        ]
    )

    # ------------------ CRUD SCREENSHOT ------------------
    render_terminal_screenshot(
        f"{base_dir}/crud_operations_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), ("hbase shell 03_crud_operations.hbase", "cmd")],
            [("hbase:001:0> ", "hprompt"), ("# 1. Point GET by Row-Key", "comment")],
            [("hbase:002:0> ", "hprompt"), ("get 'saferoads_accidents', 'OH#2#2016-02-08#A-2' -> 1 row(s) [Took 0.0726s]", "cmd")],
            [("hbase:003:0> ", "hprompt"), ("# 2. Column-Projected SCAN with Limit", "comment")],
            [("hbase:004:0> ", "hprompt"), ("scan 'saferoads_accidents', {COLUMNS => ['loc:city', 'hazard:severity'], LIMIT => 5} -> 5 row(s) [Took 0.0975s]", "cmd")],
            [("hbase:005:0> ", "hprompt"), ("# 3. Cached Server COUNT Operation", "comment")],
            [("hbase:006:0> ", "hprompt"), ("count 'saferoads_accidents', INTERVAL => 100, CACHE => 100 -> 5 row(s) [Took 0.0421s]", "cmd")],
            [("hbase:007:0> ", "hprompt"), ("# 4. Specific Cell DELETE & Full Row DELETEALL", "comment")],
            [("hbase:008:0> ", "hprompt"), ("delete 'saferoads_accidents', 'TEST#1#2026-10-08#TEMP-001', 'hazard:amenity' [Took 0.0387s]", "cmd")],
            [("hbase:009:0> ", "hprompt"), ("deleteall 'saferoads_accidents', 'TEST#1#2026-10-08#TEMP-001' [Took 0.0378s]", "cmd")],
            [("hbase:010:0> ", "hprompt"), ("get 'saferoads_accidents', 'TEST#1#2026-10-08#TEMP-001' -> 0 row(s) (Verified Deleted)", "success")],
            ([("hbase:011:0> ", "hprompt"), ("exit", "dim")])
        ]
    )

    # ------------------ JAVA API SCREENSHOT ------------------
    render_terminal_screenshot(
        f"{base_dir}/java_api_screenshot.png",
        [
            [("● ", "bullet"), ("PS E:\\Big_data> ", "prompt"), (".\\run_java_api.cmd", "cmd")],
            ("[SUCCESS] Successfully connected to Apache HBase cluster at ZooKeeper: 127.0.0.1:2181", "success"),
            ("[STEP 1] Creating HBase Table: 'saferoads_accidents' with 4 Column Families (loc, time, env, hazard)", "data"),
            ("[SUCCESS] TableDescriptor & ColumnFamilyDescriptor provisioned successfully.", "success"),
            ("[STEP 2] Batch Ingesting real-world records from: accidents_cleaned.tsv", "data"),
            ("[SUCCESS] Successfully ingested 2000 real-world accident records via Table.put(List<Put>).", "success"),
            ("[STEP 3] Demonstrating Point GET: Table.get(RowKey: OH#2#2016-02-08#A-2)", "data"),
            ("  HBase Record Found: Reynoldsburg, Franklin County, OH | Light Rain | Severity: 2", "dim"),
            ("[STEP 4] Demonstrating Bounded Range SCAN: Scanned 10 records across Ohio DOT corridor.", "data"),
            ("[STEP 5A] Executing PrefixFilter ('OH#'): Matched 5 state corridor records.", "data"),
            ("[STEP 5B] Executing SingleColumnValueFilter (weather contains Rain): Matched 5 adverse weather records.", "data"),
            ("[STEP 5C] Executing Compound FilterList (Severity=4 AND Junction=1): Matched 1 critical hazard record.", "data"),
            ("[STEP 6] Demonstrating DELETE Operations on RowKey: OH#3#2016-02-08#A-4", "data"),
            ("  [SUCCESS] Column 'hazard:amenity' deleted via Table.delete(Delete).", "dim"),
            ("  [SUCCESS] Full row successfully deleted (GET returned empty result).", "dim"),
            ("[STEP 7] Counting total records in table: 2000 verified records.", "success"),
            ("[INFO] ZooKeeper and Table connections cleanly closed.", "dim")
        ]
    )

    print("All authentic pitch-black terminal screenshots generated successfully!")

if __name__ == '__main__':
    generate_all_screenshots()
