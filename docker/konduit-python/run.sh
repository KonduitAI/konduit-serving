echo "Installing required packages..."
pip install --no-cache-dir -r requirements.txt
echo "Python packages installed successfully."

echo "-------------------------------------------------------------------"

echo "Creating config file..."
python create-config.py
echo "Config file created."

echo "-------------------------------------------------------------------"

echo "Starting konduit server..."
java "${EXTRA_ARGS}" -cp konduit.jar ai.konduit.serving.configprovider.KonduitServingMain \
  --configPath source/config.json \
  --verticleClassName ai.konduit.serving.verticles.inference.InferenceVerticle
echo "Konduit Server has started successfully."