# Script to copy all extensions from one FSH profile into another
from dataclasses import dataclass
from typing import List
import sys
import re


@dataclass
class FSHBlock:
    lines: List[str]

    def type(self):
        first_line = self.lines[0]
        if first_line.startswith("*"):
            # filter 'extension' from string like this: '* extension[xyz]'
            return re.search(r"\* (\w+)", first_line).group(1)
        elif re.search(r"^\w+:", first_line):
            return re.search(r"(\w+):", first_line).group(1)


def read_fsh_file(path: str) -> List[FSHBlock]:
    with open(path, "r") as f:
        lines = f.readlines()
    blocks = []
    current_block = []
    # if emmpty line, add it to the current block
    # if line starts with 2 spaces, add it to the current block
    # if line is not empty and does not start with 2 spaces, start a new block
    for line in lines:
        if line.strip() == "" or line.startswith("  "):
            current_block.append(line)
        else:
            if len(current_block) > 0:
                blocks.append(FSHBlock(current_block))
            current_block = [line]
    return blocks


# usage: python copy_extensions.py source_path target_path
if __name__ == "__main__":

    source_path = sys.argv[1]
    target_path = sys.argv[2]

    source_blocks = read_fsh_file(source_path)
    source_extensions = [
        block for block in source_blocks if block.type() == "extension"
    ]

    target_blocks = read_fsh_file(target_path)

    result_blocks = []

    extensions_copied = False
    for block in target_blocks:
        if block.type() == "extension" and not extensions_copied:
            result_blocks.extend(source_extensions)
            extensions_copied = True
        elif block.type() == "extension":
            continue
        else:
            result_blocks.append(block)

    if not extensions_copied:
        result_blocks.extend(source_extensions)

    with open(target_path, "w") as f:
        for block in result_blocks:
            f.writelines(block.lines)
