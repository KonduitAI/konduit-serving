from setuptools import find_packages
from setuptools import setup

setup(
    name="konduit",
    version="0.1.8",
    packages=find_packages(),
    install_requires=[
        "requests>=2.22.0",
        "numpy<=1.16.4",  # For compatibility with python 2
        "pyarrow==0.13.0",
        "requests-toolbelt>=0.9.1",
        "pandas<=0.24.2",  # For compatibility with python 2
        "Cython",
        "pydl4j",
        "pydatavec",
        "pyyaml",
        "click",
        "packaging",
        "hurry.filesize"
    ],
    py_modules=["konduit", "cli"],
    extras_require={
        "tests": ["pytest", "pytest-pep8", "pytest-cov", "mock"],
        "codegen": ["jsonschema2popo"],
        "dev": ["black", "pre-commit"],
    },
    entry_points={"console_scripts": ["konduit=cli:cli", "konduit-init=cli:init"]},
    include_package_data=True,
    license="Apache",
    description="konduit: Enterprise Runtime for Machine Learning Models",
    long_description="konduit: Enterprise Runtime for Machine Learning Models",
    author="Max Pumperla, Shams Ul Azeem",
    author_email="max@konduit.ai, shams@konduit.ai",
    url="https://github.com/KonduitAI/konduit-serving",
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Intended Audience :: Developers",
        "Environment :: Console",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
        "Programming Language :: Python",
        "Programming Language :: Python :: 3",
    ],
)
