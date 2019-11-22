import pydl4j
pydl4j.add_classpath('./target/test.jar')

from konduit import *


def markdown_blocks(file_path, language='python', safe=True):
    code_blocks = []
    code_regex = r'^```.*'
    code_block_open_re = r'^```(`*)({0}){1}$'.format(language, '' if safe else '?')

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


def exec_python_code(code_blocks):
    for block in code_blocks:
        try:
            exec(block)
        except Exception as e:
            print("Test execution failed in the following block:\n")
            print(block)
            print("failed with error message:\n")
            raise e


def exec_java_code(code_blocks):
    for block in code_blocks:
        lines = block.split('\n')
        import_lines = [l for l in lines if l.startswith('import ')]
        code_lines = [l for l in lines if not l.startswith('import ')]

        code = "package org.test;\n\n" + '\n'.join(import_lines) \
               + "\n\npublic class BasicsTest {\n" \
               + "\tpublic BasicsTest() {}\n" \
               + "\tpublic boolean smokeTest() { return true; };\n" \
               + "\tpublic void testCode() {\n\t\t" + '\n\t\t'.join(code_lines) + "\n\t}\n}"

        if not os.path.isdir('./org'):
            os.mkdir('./org')
        if not os.path.isdir('./org/test'):
            os.mkdir('./org/test')

        with open('org/test/BasicsTest.java', 'w') as f:
            f.write(code)

        subprocess.call(["./build_jar.sh"])

        basic_test = autoclass('org.test.BasicsTest')()
        basic_test.smokeTest()
        basic_test.testCode()

        os.remove('./org/test/BasicsTest.java')


def test_docs():
    files = get_files('../docs')
    for file_path in files:
        python_blocks = markdown_blocks(file_path, 'python')
        exec_python_code(python_blocks)

        java_blocks = markdown_blocks(file_path, 'java')
        exec_java_code(java_blocks)
