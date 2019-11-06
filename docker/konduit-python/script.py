# <in:x|NDARRAY,y|NDARRAY:> --- Acceptable values are [BOOL, INT, FLOAT, STR, NDARRAY]
# <out:z|NDARRAY:>
# <rest_in:NUMPY:> --- Available values are [NUMPY, JSON, RAW]
# <rest_out:NUMPY:>

import numpy as np

if 'x' not in globals():
    x = [1, 2, 3]

if 'y' not in globals():
    y = [1, 2, 3]

z = np.asarray(x) + np.asarray(y)

print(z)
