import os
import re
import glob


def markdown_python_blocks(file_path, safe=True):
    code_blocks = []
    code_regex = r'^```.*'
    code_block_open_re = r'^```(`*)(py|python){0}$'.format('' if safe else '?')

    with open(file_path, 'r') as f:
        block = []
        python = True
        in_code_block = False

        for line in f.readlines():
            code_block_delimiter = re.match(code_regex, line)

            if in_code_block:
                if code_block_delimiter:
                    if python:
                        code_blocks.append(''.join(block))
                    block = []
                    python = True
                    in_code_block = False
                else:
                    block.append(line)
            elif code_block_delimiter:
                in_code_block = True
                if not re.match(code_block_open_re, line):
                    python = False
    return code_blocks


def is_markdown(f):
    markdown_extensions = ['.markdown', '.mdown', '.mkdn', '.mkd', '.md']
    return os.path.splitext(f)[1] in markdown_extensions


def get_files(input_dir):
    return [os.path.join(dp, f) for dp, dn, filenames in os.walk(input_dir) for f in filenames if is_markdown(f)]


def test_docs():
    files = get_files('../docs')
    for file_path in files:
        code_blocks = markdown_python_blocks(file_path)
        for block in code_blocks:
            try:
                exec(block)
            except Exception as e:
                print("Test execution failed in the following block:\n")
                print(block)
                print("failed with error message:\n")
                raise e

