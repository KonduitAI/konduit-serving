import io
import time
import torchvision
from konduit.load import client_from_file


def test_model():
    client = client_from_file("konduit.yml")

    testset = torchvision.datasets.CIFAR10(
        root="./model/data", train=False, download=True
    )
    image, _ = testset[0]

    responses = []

    start = time.time()
    for i in range(10):
        byte_buf = io.BytesIO()
        image.save(byte_buf, format="PNG")
        byte_array = byte_buf.getvalue()
        responses.append(client.predict({"default": byte_array}))
    print(len(responses))
    end = time.time()

    print(
        "%f seconds elapsed for 1000 requests (%d RPS)"
        % (end - start, (10.0 / (end - start)))
    )


if __name__ == "__main__":
    test_model()
