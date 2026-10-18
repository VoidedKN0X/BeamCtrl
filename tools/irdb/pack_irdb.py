#!/usr/bin/env python3
"""
Packs Flipper-IRDB (https://github.com/Lucaslhm/Flipper-IRDB) into the code library
BeamCtrl ships with, so the app can browse it without going online.

    git clone --depth 1 https://github.com/Lucaslhm/Flipper-IRDB /tmp/irdb
    python3 tools/irdb/pack_irdb.py /tmp/irdb app/src/main/assets/irdb

Every signal goes through the same checks the app uses for imported files: only protocols
BeamCtrl can send, carrier 10-500 kHz, at most 2,048 timings, no period over one second,
nothing longer than two seconds, at most 500 signals per device. Devices with nothing
sendable left are dropped, and identical files are kept once.

Writes:
  index.tsv    one line per device: category, brand, model, signal count, has power, offset, length
  data.bin     each device's cleaned .ir text, raw-deflated, back to back (stored uncompressed in
               the APK so the app can jump straight to one device)
  tv_power.ir  every distinct TV power code, most widely used first, for universal power-off
"""
import hashlib
import os
import re
import subprocess
import sys
import zlib
from collections import Counter, OrderedDict
from datetime import date

PROTOCOLS = {
    "nec": "NEC", "necext": "NECext", "samsung32": "Samsung32",
    "sirc": "SIRC", "sirc15": "SIRC15", "sirc20": "SIRC20",
    "rc5": "RC5", "rc5x": "RC5X", "rc6": "RC6",
}
MAX_SIGNALS = 500
MAX_TIMINGS = 2048
MAX_TIMING_US = 1_000_000
MAX_PATTERN_US = 2_000_000
MAX_LABEL = 60

CATEGORY_NAMES = {
    "TVs": "TVs",
    "SoundBars": "Soundbars",
    "Speakers": "Speakers",
    "Audio_and_Video_Receivers": "AV receivers",
    "Projectors": "Projectors",
    "Streaming_Devices": "Streaming devices",
    "Cable_Boxes": "Cable and satellite boxes",
    "DVB-T": "DVB-T receivers",
    "TV_Tuner": "TV tuners",
    "Blu-Ray": "Blu-ray players",
    "DVD_Players": "DVD players",
    "CD_Players": "CD players",
    "VCR": "VCRs",
    "Laserdisc": "Laserdisc players",
    "MiniDisc": "MiniDisc players",
    "Monitors": "Monitors",
    "Touchscreen_Displays": "Touchscreen displays",
    "Digital_Signs": "Digital signs",
    "Whiteboards": "Whiteboards",
    "Videoconferencing": "Videoconferencing",
    "Multimedia": "Multimedia",
    "Consoles": "Game consoles",
    "Computers": "Computers",
    "KVM": "KVM switches",
    "Converters": "Converters and switches",
    "Car_Multimedia": "Car multimedia",
    "Head_Units": "Car head units",
    "Cameras": "Cameras",
    "CCTV": "CCTV",
    "ACs": "Air conditioners",
    "Fans": "Fans",
    "Heaters": "Heaters",
    "Fireplaces": "Fireplaces",
    "Air_Purifiers": "Air purifiers",
    "Humidifiers": "Humidifiers",
    "Dust_Collectors": "Dust collectors",
    "LED_Lighting": "Lights",
    "Clocks": "Clocks",
    "Picture_Frames": "Picture frames",
    "Vacuum_Cleaners": "Vacuum cleaners",
    "Window_cleaners": "Window cleaners",
    "Bidet": "Bidets",
    "Handicap_Ceiling_Lifts": "Ceiling lifts",
    "Toys": "Toys",
    "Universal_TV_Remotes": "Universal TV remotes",
    "Miscellaneous": "Miscellaneous",
    "_Converted_": "More devices",
}
# Shown first, in this order; everything else follows alphabetically, "More devices" last.
CATEGORY_ORDER = ["TVs", "SoundBars", "Audio_and_Video_Receivers", "Projectors", "Streaming_Devices",
                  "Cable_Boxes", "ACs", "Fans", "LED_Lighting"]

POWER_NAMES = {"power", "pwr", "poweron", "onoff", "powertoggle", "tvpower", "tvpwr", "powertv", "tvonoff", "poweroff"}
# Converted files aren't sorted by device type, so TVs among them are picked out by name.
TV_PATH = re.compile(r"(^|[^a-z])(tv|television|lcd|led ?tv|plasma)([^a-z]|$)")


def clean(text, limit=MAX_LABEL):
    text = "".join(ch for ch in text if ch.isprintable() and ch not in "\t\r\n")
    return re.sub(r"\s+", " ", text).strip()[:limit].strip()


def is_power(name):
    n = re.sub(r"[^a-z0-9]", "", name.lower())
    return n in POWER_NAMES


def parse_blocks(text):
    blocks, cur = [], OrderedDict()

    def flush():
        nonlocal cur
        if "name" in cur:
            blocks.append(cur)
        cur = OrderedDict()

    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("#"):
            flush()
        elif not line:
            continue
        else:
            i = line.find(":")
            if i > 0:
                key = line[:i].strip().lower()
                if key == "name" and "name" in cur:
                    flush()
                cur[key] = line[i + 1:].strip()
    flush()
    return blocks


def hex_bytes(value):
    parts = value.split()
    if not parts or len(parts) > 4:
        return None
    try:
        return [int(p, 16) & 0xFF for p in parts]
    except ValueError:
        return None


def normalise(block):
    """Returns (name, canonical text, key for de-duplicating the code) or None if BeamCtrl can't send it."""
    name = clean(block.get("name", "")) or "Button"
    kind = block.get("type", "").lower()
    if kind == "parsed":
        proto = PROTOCOLS.get(block.get("protocol", "").lower())
        address, command = hex_bytes(block.get("address", "")), hex_bytes(block.get("command", ""))
        if not proto or address is None or command is None:
            return None
        a = " ".join(f"{b:02X}" for b in (address + [0] * 4)[:4])
        c = " ".join(f"{b:02X}" for b in (command + [0] * 4)[:4])
        body = f"type: parsed\nprotocol: {proto}\naddress: {a}\ncommand: {c}\n"
        return name, body, ("p", proto, a, c)
    if kind == "raw":
        try:
            freq = int(float(block.get("frequency", "38000")))
        except ValueError:
            return None
        if not 10_000 <= freq <= 500_000:
            return None
        timings = []
        for t in block.get("data", "").split():
            try:
                v = int(t)
            except ValueError:
                continue
            if v > 0:
                timings.append(v)
        if not timings or len(timings) > MAX_TIMINGS or max(timings) > MAX_TIMING_US or sum(timings) > MAX_PATTERN_US:
            return None
        data = " ".join(map(str, timings))
        body = f"type: raw\nfrequency: {freq}\ndata: {data}\n"
        # Learned raw codes of the same button never match exactly; round so near-identical ones merge.
        key = ("r", round(freq, -3), tuple(round(t, -2) for t in timings))
        return name, body, key
    return None


def pretty(stem):
    return clean(stem.replace("_", " "))


def describe(rel):
    """(category folder, brand, model) for a path inside the IRDB."""
    parts = rel.split("/")
    cat = parts[0]
    stem = os.path.splitext(parts[-1])[0]
    if cat == "_Converted_":
        # _Converted_/<source>/<letter>/<Brand>/[<device>/]<file>.ir
        brand = pretty(parts[3]) if len(parts) > 4 else pretty(stem.split("_")[0])
        extra = [pretty(p) for p in parts[4:-1]]
    else:
        brand = pretty(parts[1]) if len(parts) > 2 else pretty(stem.split("_")[0])
        extra = [pretty(p) for p in parts[2:-1]]
    model = pretty(stem)
    # "Samsung_BN59-01179A_TV" under Samsung reads better as "BN59-01179A TV".
    if brand and model.lower().startswith(brand.lower()):
        rest = model[len(brand):].strip(" -_")
        model = rest or model
    if extra:
        model = clean(" ".join(extra + [model]), 120)
    return cat, brand or "Unknown", model or "Remote"


def main(src, out):
    os.makedirs(out, exist_ok=True)
    commit = subprocess.run(["git", "-C", src, "rev-parse", "--short", "HEAD"], capture_output=True, text=True).stdout.strip() or "unknown"

    files = []
    for root, _, names in os.walk(src):
        if "/.git" in root:
            continue
        for n in names:
            if n.lower().endswith(".ir"):
                rel = os.path.relpath(os.path.join(root, n), src).replace(os.sep, "/")
                files.append(rel)
    files.sort(key=lambda r: (r.startswith("_Converted_"), r.lower()))

    devices, seen, stats = [], set(), Counter()
    power_votes, power_bodies, power_brands = Counter(), {}, {}
    for rel in files:
        if rel == "_Converted_/Power_off.ir":
            continue  # a power-code list, not a device; universal power-off covers it
        with open(os.path.join(src, rel), "rb") as f:
            text = f.read(4 * 1024 * 1024 + 1).decode("utf-8", "replace")
        signals = []
        for block in parse_blocks(text):
            n = normalise(block)
            stats["signals"] += 1
            if n is None:
                stats["skipped"] += 1
                continue
            signals.append(n)
            if len(signals) >= MAX_SIGNALS:
                break
        if not signals:
            stats["empty_files"] += 1
            continue
        body = "Filetype: IR signals file\nVersion: 1\n" + "".join(f"#\nname: {name}\n{b}" for name, b, _ in signals)
        digest = hashlib.sha1(body.encode("utf-8")).digest()
        if digest in seen:
            stats["duplicates"] += 1
            continue
        seen.add(digest)
        cat, brand, model = describe(rel)
        has_power = any(is_power(name) for name, _, _ in signals)
        devices.append((cat, brand, model, len(signals), has_power, body))

        # TV power codes for universal power-off: one vote per file that uses the code.
        if cat in ("TVs", "Universal_TV_Remotes") or (cat == "_Converted_" and TV_PATH.search(rel.lower())):
            file_power = {key: b for name, b, key in signals if is_power(name)}
            for key, b in file_power.items():
                power_votes[key] += 1
                power_bodies.setdefault(key, b)
                power_brands.setdefault(key, Counter())[brand] += 1

    cats = sorted({d[0] for d in devices}, key=lambda c: (
        c == "_Converted_",
        CATEGORY_ORDER.index(c) if c in CATEGORY_ORDER else len(CATEGORY_ORDER),
        CATEGORY_NAMES.get(c, c).lower(),
    ))
    cat_index = {c: i for i, c in enumerate(cats)}
    devices.sort(key=lambda d: (cat_index[d[0]], d[1].lower(), d[2].lower()))

    with open(os.path.join(out, "data.bin"), "wb") as data, open(os.path.join(out, "index.tsv"), "w", encoding="utf-8") as index:
        index.write(f"BeamCtrl-IRDB\t1\t{commit}\t{date.today().isoformat()}\n")
        for c in cats:
            index.write(f"C\t{CATEGORY_NAMES.get(c, pretty(c))}\n")
        offset = 0
        for cat, brand, model, count, has_power, body in devices:
            z = zlib.compressobj(9, zlib.DEFLATED, -15)
            blob = z.compress(body.encode("utf-8")) + z.flush()
            data.write(blob)
            index.write(f"D\t{cat_index[cat]}\t{brand}\t{model}\t{count}\t{1 if has_power else 0}\t{offset}\t{len(blob)}\n")
            offset += len(blob)

    # Codes used by more TVs go first, so a sweep finds the common TVs quickly. Built-in brand
    # codes are always in the list because they come from the same database.
    ranked = sorted(power_votes, key=lambda k: (-power_votes[k], k[0] != "p"))
    with open(os.path.join(out, "tv_power.ir"), "w", encoding="utf-8") as f:
        f.write("Filetype: IR signals file\nVersion: 1\n")
        for key in ranked[:MAX_SIGNALS]:
            brand = power_brands[key].most_common(1)[0][0]
            f.write(f"#\nname: {clean(brand)}\n{power_bodies[key]}")

    size = os.path.getsize(os.path.join(out, "data.bin"))
    print(f"IRDB {commit}: {len(devices)} devices in {len(cats)} categories, data {size / 1e6:.1f} MB")
    print(f"signals {stats['signals']}, skipped {stats['skipped']}, empty files {stats['empty_files']}, duplicates {stats['duplicates']}")
    print(f"TV power codes: {min(len(ranked), MAX_SIGNALS)} of {len(ranked)}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
