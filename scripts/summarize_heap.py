"""Read Android/standard HPROF and emit class counts, never object/string contents.

This is an instance-count summary, not a retained-object reachability/leak analyzer.
Keep raw heaps private: Firebase tokens may be present in process memory.
"""
import argparse
import collections
import json
import struct
from pathlib import Path


def summarize(path):
    data = memoryview(path.read_bytes())
    header_end = bytes(data[:100]).index(b"\0") + 1
    id_size = struct.unpack_from(">I", data, header_end)[0]
    offset = header_end + 12
    strings, class_names = {}, {}
    counts = collections.Counter()
    sizes = {2: id_size, 4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}

    def number(position, size):
        return int.from_bytes(data[position:position+size], "big")

    roots = {0xff: id_size, 1: 2*id_size, 2: id_size+8, 3: id_size+8,
             4: id_size+4, 5: id_size, 6: id_size+4, 7: id_size, 8: id_size+8,
             0x89: id_size, 0x8a: id_size, 0x8b: id_size, 0x8c: id_size,
             0x8d: id_size, 0x8e: id_size+8, 0x90: id_size, 0xfe: 4+id_size}
    while offset < len(data):
        tag = data[offset]
        length = number(offset+5, 4)
        p, end = offset+9, offset+9+length
        if tag == 1:
            strings[number(p, id_size)] = bytes(data[p+id_size:end]).decode("utf-8", "replace")
        elif tag == 2:
            class_names[number(p+4, id_size)] = number(p+8+id_size, id_size)
        elif tag in (0x0c, 0x1c):
            while p < end:
                sub = data[p]
                p += 1
                if sub in roots:
                    p += roots[sub]
                elif sub == 0x20:
                    p += 7*id_size + 8
                    constant_count = number(p, 2)
                    p += 2
                    for _ in range(constant_count):
                        value_type = data[p+2]
                        p += 3 + sizes[value_type]
                    static_count = number(p, 2)
                    p += 2
                    for _ in range(static_count):
                        value_type = data[p+id_size]
                        p += id_size + 1 + sizes[value_type]
                    field_count = number(p, 2)
                    p += 2 + field_count*(id_size+1)
                elif sub == 0x21:
                    class_id = number(p+id_size+4, id_size)
                    size = number(p+2*id_size+4, 4)
                    counts[class_id] += 1
                    p += 2*id_size+8+size
                elif sub == 0x22:
                    count = number(p+id_size+4, 4)
                    p += 2*id_size+8+count*id_size
                elif sub in (0x23, 0xc3):
                    count = number(p+id_size+4, 4)
                    value_type = data[p+id_size+8]
                    p += id_size+9+(count*sizes[value_type] if sub == 0x23 else 0)
                else:
                    raise ValueError(f"Unknown HPROF subrecord {sub:#x} at {p-1}")
        offset = end
    selected = {}
    for class_id, name_id in class_names.items():
        name = strings.get(name_id, "").replace("/", ".")
        if name.startswith("vn.edu.vlu.milestone2.") and (
                name.endswith("Fragment") or name.endswith("MainActivity")):
            selected[name] = counts[class_id]
    return {"heap_bytes": len(data), "application_instance_counts": dict(sorted(selected.items())),
            "scope": "Class instance counts after Android heap capture; not a global leak verdict"}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("heap", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = summarize(args.heap)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))
