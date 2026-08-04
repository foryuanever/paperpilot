import os
import sys
import json
import argparse
from modelscope.hub.snapshot_download import snapshot_download

def progress_check(local_dir, lite_mode):
    # Estimated size of files we expect
    # Lite mode is around 256MB. Full mode is around 1000MB (1.0GB).
    expected_size = 256 * 1024 * 1024 if lite_mode else 1000 * 1024 * 1024
    
    total_downloaded = 0
    if os.path.exists(local_dir):
        for root, dirs, files in os.walk(local_dir):
            for file in files:
                # ignore temp files in size count
                if not file.endswith('.tmp'):
                    total_downloaded += os.path.getsize(os.path.join(root, file))
                    
    percentage = min(99, int((total_downloaded / expected_size) * 100))
    return percentage

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--lite', action='store_true', help='Download in lite mode (exclude heavy MFR models)')
    args = parser.parse_args()

    # Target directory for PDF-Extract-Kit
    cache_dir = os.path.expanduser('~/.cache/modelscope/hub')
    model_id = "OpenDataLab/PDF-Extract-Kit-1.0"
    
    ignore_patterns = []
    if args.lite:
        # Exclude unimernet formula model (~770MB) and optionally table reconstruction SlanetPlus (~150MB)
        ignore_patterns = [
            "models/MFR/*",
            "models/TabRec/SlanetPlus/*"
        ]
        print(f"STATUS:Downloading models in LITE mode...", flush=True)
    else:
        print(f"STATUS:Downloading models in FULL mode...", flush=True)

    # Monitor download in background/loop if needed, but snapshot_download is blocking.
    # We will print periodic updates based on callback or simple output logs.
    print(f"STATUS:Connecting to ModelScope model repository...", flush=True)
    
    try:
        # Perform download
        downloaded_path = snapshot_download(
            model_id,
            ignore_file_pattern=ignore_patterns,
            cache_dir=cache_dir
        )
        print(f"SUCCESS:Models downloaded and saved to {downloaded_path}", flush=True)
    except Exception as e:
        print(f"ERROR:Model download failed: {str(e)}", file=sys.stderr, flush=True)
        sys.exit(1)

if __name__ == '__main__':
    main()
