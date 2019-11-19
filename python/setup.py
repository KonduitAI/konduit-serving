from setuptools import setup
from setuptools import find_packages

setup(
    name='konduit',
    version='0.1.4',
    packages=find_packages(),
    install_requires=[
        'requests>=2.22.0',
        'numpy<=1.16.4',  # For compatibility with python 2
        'pyarrow==0.13.0',
        'requests-toolbelt>=0.9.1',
        'pandas<=0.24.2',  # For compatibility with python 2
        'Cython',
        'pydl4j',
        'pydatavec',
        'click',
        'pyyaml'
    ],
    py_modules=['konduit', 'cli'],
    extras_require={
        'tests': ['pytest', 'pytest-pep8', 'pytest-cov', 'mock'],
        'codegen': ['jsonschema2popo']
    },
    entry_points={
        'console_scripts': [
            'konduit=cli:cli'
        ]
    },
    include_package_data=True,
    license='Apache',
    description='konduit: Enterprise Runtime for Machine Learning Models',
    long_description='konduit: Enterprise Runtime for Machine Learning Models',
    author='Max Pumperla',
    author_email='max@skymind.global',
    url='https://github.com/KonduitAI/konduit-serving',
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'Environment :: Console',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent',
        'Programming Language :: Python',
        'Programming Language :: Python :: 3'
    ]
)
