echo "Installing required packages..."
pip install --no-cache-dir -r requirements.txt
echo "Python packages installed successfully."

echo "-------------------------------------------------------------------"

echo "Starting konduit server..."
python start-server.py
echo "Konduit Server has started successfully."