import client

ks_client = client.KonduitServingClient('server')

ks_client.predict({
    "image": ks_client.get_image("./images/test_input_number_0.png")
})
