import client

ks_client = client.KonduitServingClient('server2')

ks_client.predict({
    "input": [[1, 2, 3, 4]]
})
