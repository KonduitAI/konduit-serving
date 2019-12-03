from konduit import *
import glob
import re
import pytest


def markdown_blocks(file_path, language="python"):
    """Get language-specific markdown code blocks.
    :param file_path: path to markdown file
    :param language: 'python', 'java' (or literally any valid code fence language)
    :return: list of code as string
    """
    code_blocks = []
    code_regex = r"^```.*"
    code_block_open_re = r"^```(`*)({0})$".format(language)

    with open(file_path, "r") as f:
        block = []
        python = True
        in_code_block = False

        for line in f.readlines():
            code_block_delimiter = re.match(code_regex, line)

            if in_code_block:
                if code_block_delimiter:
                    if python:
                        code_blocks.append("".join(block))
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
    """Does this file have a markdown extension?"""
    markdown_extensions = [".markdown", ".mdown", ".mkdn", ".mkd", ".md"]
    return os.path.splitext(f)[1] in markdown_extensions


def get_files(input_dir):
    """Get all markdown files in a directory recursively."""
    return [
        os.path.join(dp, f)
        for dp, dn, file_names in os.walk(input_dir)
        for f in file_names
        if is_markdown(f)
    ]


def exec_python_code(code_blocks):
    """Execute the Python code contained in blocks.

    :param code_blocks: List of strings containing executable Python code
    """
    for block in code_blocks:
        try:
            exec(block)
        except Exception as ex:
            print("Test execution failed in the following block:\n")
            print(block)
            print("failed with error message:\n")
            raise ex


def make_folders():
    """Make base folders for Java application logic."""
    if not os.path.isdir("./ai/konduit/serving"):
        os.makedirs("./ai/konduit/serving")


def is_self_contained_example(block):
    """Basic sanity check if markdown block contains a class and a main method."""
    return "public static void main" in block and "public class" in block


def write_self_contained_example(block):
    """Write the full code block to a file if we're dealing with a self-contained
    example.

    :param block: code block as string
    """
    class_regex = r"public\s+class\s+(\w+)"
    class_name = re.search(class_regex, block)
    if class_name:
        class_name = class_name.group(1)
        with open("ai/konduit/serving/" + class_name + ".java", "w") as f:
            f.write(block)
    else:
        raise Exception("Could not determine proper class name")


def write_example_from_snippet(block, markdown_root, i):
    """Write imports first and supply main code snippets to a method in a constructed Java class
    :param block: code block
    :param markdown_root: name of the file this snippet comes from (without extension)
    :param i: this block is the i-th example  in the current markdown file
    """
    lines = block.split("\n")
    import_lines = [l for l in lines if l.startswith("import ")]
    code_lines = [l for l in lines if not l.startswith("import ")]
    class_name = "BasicsTest" + markdown_root + str(i)

    code = (
        "package ai.konduit.serving;\n\n"
        + "\n".join(import_lines)
        + "\n\npublic class "
        + class_name
        + " {\n"
        + "\tpublic "
        + class_name
        + " () {}\n"
        + "\tpublic void main() {\n\t\t"
        + "\n\t\t".join(code_lines)
        + "\n\t}\n}"
    )

    with open("ai/konduit/serving/" + class_name + ".java", "w") as f:
        f.write(code)


def get_markdown_root(file_path):
    full_path = os.path.splitext(file_path)[0]
    return full_path.split(os.sep)[-1]


def write_java_files(code_blocks, file_path):
    """Write code contained in blocks into proper Java source files.
    We cover two basic scenarios:
        1. the code is a self-contained Java app with main method
        2. we just have a small snippet of imports and statements (that run when correctly
           (transformed into a class)

    :param code_blocks: code blocks as list of string
    :param file_path: path to file containing the code
    """
    markdown_root = get_markdown_root(file_path)
    make_folders()
    for i, block in enumerate(code_blocks):
        if is_self_contained_example(block):
            write_self_contained_example(block)
        else:
            write_example_from_snippet(block, markdown_root, i)


def exec_java_code(code_blocks, file_path):
    """Execute the java classes previously put into the konduit.jar by the
    `prepare_doc_tests.sh` script.

    :param code_blocks: code blocks as list of string
    :param file_path: path to file containing the code
    :return:
    """
    markdown_root = get_markdown_root(file_path)

    for i, block in enumerate(code_blocks):
        if is_self_contained_example(block):
            class_regex = r"public\s+class\s+(\w+)"
            class_name = re.search(class_regex, block).group(1)
        else:
            class_name = "BasicsTest{}{}".format(markdown_root, str(i))
        basic_test_class = autoclass("ai.konduit.serving." + class_name)
        basic_test = basic_test_class()
        if is_self_contained_example(block):
            basic_test.main([])
        else:
            basic_test.main()


def clean_java_files(ext=""):
    """Clean Java source files and classes"""
    for file_name in glob.glob("./ai/konduit/serving/*" + ext):
        try:
            os.remove(file_name)
        except FileNotFoundError:
            continue


@pytest.mark.unit
def test_docs():
    """This is the main unit test for testing documentation code snippets
    contained in markdown files."""
    files = get_files("../docs")
    for file_path in files:
        python_blocks = markdown_blocks(file_path, "python")
        exec_python_code(python_blocks)

        java_blocks = markdown_blocks(file_path, "java")
        exec_java_code(java_blocks, file_path)
        clean_java_files()


def _prepare_docs_jar():
    """This helper will be called from the `prepare_doc_tests.sh` script."""
    files = get_files("../docs")
    for file_path in files:
        java_blocks = markdown_blocks(file_path, "java")
        write_java_files(java_blocks, file_path)


if __name__ == "__main__":
    print("prepare java files")
    _prepare_docs_jar()
