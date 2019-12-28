import os
import shutil
import sys

path = os.path.join(os.path.expanduser('~'), '.javacpp', 'cache', 'numpy-{}-{}.jar'.format(sys.argv[1], sys.argv[2]))

print("Attempting to delete numpy javacpp cache at: {}".format(path))

if not os.path.isdir(path):
    print('Numpy javacpp cache at {} does not exist. Continuing with normal process flow...'.format(path))
else:
    shutil.rmtree(path)
    if not os.path.exists(path):
        print('Successfully deleted numpy javacpp cache at: {}'.format(path))
    else:
        print('Unable to delete numpy javacpp cache at: {}'.format(path))
