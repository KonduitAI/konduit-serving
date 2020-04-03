import torch
import torchvision.transforms as transforms

from .model.train import Model

transform = None
model = None
first = None
second = None


def setup():
    global transform
    global model

    transform = transforms.Compose(
        [  # transforms.ToTensor(),
            transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))
        ]
    )
    model = Model()
    model.load_state_dict(torch.load("./model/model.pt"))
    model.eval()


def run():
    global transform
    global model
    global first
    global second

    input_data = transform(torch.from_numpy(first).squeeze(0)).unsqueeze(0)
    second = model(input_data).detach().numpy()


setup()
run()
