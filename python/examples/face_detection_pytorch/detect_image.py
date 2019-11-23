import os
from vision.ssd.config.fd_config import define_img_size
from vision.ssd.mb_tiny_RFB_fd import create_Mb_Tiny_RFB_fd, create_Mb_Tiny_RFB_fd_predictor
from utils import base64_to_ndarray

threshold = 0.7
candidate_size = 1500
define_img_size(640)
test_device = 'cpu'

work_dir = os.path.abspath('./Ultra-Light-Fast-Generic-Face-Detector-1MB')
label_path = os.path.join(work_dir, "models/voc-model-labels.txt")
test_device = test_device

class_names = [name.strip() for name in open(label_path).readlines()]

model_path = os.path.join(work_dir, "models/pretrained/version-RFB-320.pth")
net = create_Mb_Tiny_RFB_fd(len(class_names), is_test=True, device=test_device)
predictor = create_Mb_Tiny_RFB_fd_predictor(net, candidate_size=candidate_size, device=test_device)
net.load(model_path)

# The variable "image" is sent from the konduit client in "konduit_server.py", i.e. in our example "encoded_image"
image = base64_to_ndarray(image)
boxes, _, _ = predictor.predict(image, candidate_size / 2, threshold)

# "image" is then picked up again from here and returned to the client
image = str(len(boxes))

