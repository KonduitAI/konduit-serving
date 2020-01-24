import os
import sys
from PIL import Image
work_dir = os.path.abspath("src/test/resources/scripts/face_detection_pytorch/Ultra-Light-Fast-Generic-Face-Detector-1MB")
#work_dir = os.path.abspath("Ultra-Light-Fast-Generic-Face-Detector-1MB")
sys.path.append(work_dir)
print("work_dir",work_dir)

from vision.ssd.config.fd_config import define_img_size
from vision.ssd.mb_tiny_RFB_fd import (
    create_Mb_Tiny_RFB_fd,
    create_Mb_Tiny_RFB_fd_predictor,
)
import numpy as np
import json
# read file
with open(JsonInput, 'r') as myfile:
    data=myfile.read()

# parse file
obj = json.loads(data)
print("ImagePath: " + str(obj['ImagePath']))
ImgPath = obj['ImagePath']
ImgPath1=work_dir+'//'+str(ImgPath[0])

# This will print 2, which is the value of bias that we saved
img = np.invert(Image.open(ImgPath1))
print("Open Image",img.shape)

threshold = 0.7
candidate_size = 1500
define_img_size(640)
test_device = "cpu"

label_path = os.path.join(work_dir, "models/voc-model-labels.txt")
test_device = test_device

class_names = [name.strip() for name in open(label_path).readlines()]

model_path = os.path.join(work_dir, "models/pretrained/version-RFB-320.pth")
net = create_Mb_Tiny_RFB_fd(len(class_names), is_test=True, device=test_device)
predictor = create_Mb_Tiny_RFB_fd_predictor(
    net, candidate_size=candidate_size, device=test_device
)
net.load(model_path)

# The variable "image" is sent from the konduit client in "konduit_server.py", i.e. in our example "encoded_image"
print(">>> Server side shape")

image = np.squeeze(img)
print("image Shape ----",image.shape)
boxes, _, _ = predictor.predict(image, candidate_size / 2, threshold)

# "num_boxes" is then picked up again from here and returned to the client
num_boxes = np.array(len(boxes))
