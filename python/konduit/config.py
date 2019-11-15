import os


USER_PATH = os.path.expanduser('~')
KONDUIT_BASE_DIR = os.path.join(USER_PATH, '.konduit')
KONDUIT_DIR = os.path.join(KONDUIT_BASE_DIR, 'konduit-serving')
KONDUIT_PID_STORAGE = os.path.join(KONDUIT_DIR, 'pid.json')
