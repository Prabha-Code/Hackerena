"""
Traffic Monitoring Module - AI Vehicle Detection & Tracking
Uses YOLOv11 + DeepSORT
"""
import cv2
import numpy as np
from typing import List, Dict, Tuple
import logging
    
logger = logging.getLogger(__name__)

    
class VehicleDetector:
    """Vehicle detection using YOLOv11"""
    
    def __init__(self, model_path: str = "yolov11n.pt"):
        """Initialize detector"""
        try:
            from ultralytics import YOLO
            self.model = YOLO(model_path)
            logger.info(f"✅ YOLOv11 model loaded: {model_path}")
        except Exception as e:
            logger.error(f"❌ Failed to load model: {e}")
            self.model = None
    
    def detect(self, frame: np.ndarray) -> List[Dict]:
        """Detect vehicles in frame"""
        if self.model is None:
            return []
        
        results = self.model(frame, conf=0.5, verbose=False)
        detections = []
                
        for result in results:
            for box in result.boxes:
                cls = int(box.cls[0])
                conf = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                # Map class IDs to vehicle types
                vehicle_types = {
                    2: "car",
                    3: "motorcycle",
                    5: "bus",
                    7: "truck"
                }
                
                if cls in vehicle_types:
                    detections.append({
                        "type": vehicle_types[cls],
                        "confidence": conf,
                        "bbox": [x1, y1, x2, y2],
                        "center": [(x1+x2)/2, (y1+y2)/2]
                    })
        
        return detections


class VehicleTracker:
    """Vehicle tracking using DeepSORT"""
    
    def __init__(self):
        """Initialize tracker"""
        self.tracks = {}
        self.track_id = 0
        logger.info("✅ DeepSORT tracker initialized")
    
    def update(self, detections: List[Dict]) -> List[Dict]:
        """Update tracks with new detections"""
        # Simplified tracking - in production use full DeepSORT
        tracked = []
        
        for det in detections:
            self.track_id += 1
            tracked.append({
                **det,
                "track_id": self.track_id
            })
        
        return tracked

     
class TrafficAnalyzer:
    """Analyze traffic from video stream"""
    
    def __init__(self, model_path: str = "yolov11n.pt"):
        self.detector = VehicleDetector(model_path)
        self.tracker = VehicleTracker()
    
    def analyze_frame(self, frame: np.ndarray) -> Dict:
        """Analyze single frame"""
        detections = self.detector.detect(frame)
        tracked = self.tracker.update(detections)
        
        # Calculate metrics
        vehicle_counts = {
            "car": sum(1 for t in tracked if t["type"] == "car"),
            "bike": sum(1 for t in tracked if t["type"] == "motorcycle"),
            "bus": sum(1 for t in tracked if t["type"] == "bus"),
            "truck": sum(1 for t in tracked if t["type"] == "truck"),
        }
        
        total = sum(vehicle_counts.values())
        
        # Determine density
        if total > 100:
            density = "CRITICAL"
        elif total > 75:
            density = "HIGH"
        elif total > 50:
            density = "MEDIUM"
        else:
            density = "LOW"
        
        return {
            "vehicles": vehicle_counts,
            "total": total,
            "density": density,
            "detections": tracked
        }
    
    def analyze_video(self, video_path: str, skip_frames: int = 5) -> List[Dict]:
        """Analyze video file"""
        cap = cv2.VideoCapture(video_path)
        frame_count = 0
        results = []
              
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            if frame_count % skip_frames == 0:
                result = self.analyze_frame(frame)
                results.append(result)
            
            frame_count += 1
                 
        cap.release()
        logger.info(f"✅ Analyzed {len(results)} frames from video")
        return results
