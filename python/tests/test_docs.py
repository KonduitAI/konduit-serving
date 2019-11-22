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
    return [os.path.join(dp, f) for dp, dn, file_names in os.walk(input_dir) for f in file_names if is_markdown(f)]


def exec_python_code(code_blocks):
    for block in code_blocks:
        try:
            exec(block)
        except Exception as e:
            print("Test execution failed in the following block:\n")
            print(block)
            print("failed with error message:\n")
            raise e


def make_folders():
    if not os.path.isdir('./ai'):
        os.mkdir('./ai')
    if not os.path.isdir('./ai/konduit'):
        os.mkdir('./ai/konduit')
    if not os.path.isdir('./ai/konduit/serving'):
        os.mkdir('./ai/konduit/serving')


def write_java_files(code_blocks):
    make_folders()
    for i, block in enumerate(code_blocks):
        lines = block.split('\n')
        import_lines = [l for l in lines if l.startswith('import ')]
        code_lines = [l for l in lines if not l.startswith('import ')]

        code = "package ai.konduit.serving;\n\n" + '\n'.join(import_lines) \
               + "\n\npublic class BasicsTest" + str(i) + " {\n" \
               + "\tpublic BasicsTest" + str(i) + " () {}\n" \
               + "\tpublic boolean smokeTest() { return true; };\n" \
               + "\tpublic void testCode() {\n\t\t" + '\n\t\t'.join(code_lines) + "\n\t}\n}"

        # write all java source files
        with open('ai/konduit/serving/BasicsTest' + str(i) + '.java', 'w') as f:
            f.write(code)


def exec_java_code(code_blocks):

    for i, block in enumerate(code_blocks):
        basic_test_class = autoclass('ai.konduit.serving.BasicsTest' + str(i))
        basic_test = basic_test_class()
        basic_test.smokeTest()
        basic_test.testCode()


def clean_java_files(code_blocks):
    for i, block in enumerate(code_blocks):
        try:
            os.remove('./ai/konduit/serving/BasicsTest' + str(i) + '.java')
        except:
            pass
        try:
            os.remove('./ai/konduit/serving/BasicsTest' + str(i) + '.class')
        except:
            pass


def test_docs():
    files = get_files('../docs')
    for file_path in files:
        python_blocks = markdown_blocks(file_path, 'python')
        exec_python_code(python_blocks)

        java_blocks = markdown_blocks(file_path, 'java')
        exec_java_code(java_blocks)
        clean_java_files(java_blocks)


def prepare_docs_jar():
    files = get_files('../docs')
    for file_path in files:
        java_blocks = markdown_blocks(file_path, 'java')
        write_java_files(java_blocks)


if __name__ == "__main__":
    print("prepare java files")
    prepare_docs_jar()
