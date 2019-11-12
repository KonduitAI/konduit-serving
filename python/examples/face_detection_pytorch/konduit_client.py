from konduit.client import Client
import base64
import os

client = Client(input_names=['default'],
                output_names=['default'],
                input_type='JSON',
                return_output_type='JSON',
                endpoint_output_type='RAW',
                url='http://localhost:1337')


def to_base_64(filename):
    with open(filename, "rb") as f:
        img_base64 = base64.b64encode(f.read())
    return str(img_base64)[2:-1]

workdir = os.path.abspath('./Ultra-Light-Fast-Generic-Face-Detector-1MB')
encoded_image = to_base_64(os.path.join(workdir, 'imgs/1.jpg'))

try:
    predicted = client.predict( {'default': encoded_image})
    print(predicted)
except Exception as e:
    print(e)
