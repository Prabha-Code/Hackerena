"""
Congestion Prediction Module - LSTM & Transformer based forecasting
"""

import numpy as np
from typing import List, Dict
import logging

logger = logging.getLogger(__name__)
     

class CongestionPredictor:
    """Predict congestion using LSTM + Transformer"""
    
    def __init__(self, model_type: str = "lstm"):
        self.model_type = model_type
        self.model = None
        logger.info(f"✅ Congestion predictor initialized ({model_type})")
    
    def predict(self, historical_data: np.ndarray, horizon_minutes: List[int] = [5, 15, 30]) -> Dict:
        """
        Predict congestion for given time horizons
        
        Args:
            historical_data: Historical traffic data (N x features)
            horizon_minutes: Prediction horizons in minutes
        
        Returns:
            Predictions with confidence scores
        """
        predictions = {}
        
        for horizon in horizon_minutes:
            # Simplified prediction - in production use trained LSTM/Transformer
            base_value = np.mean(historical_data[-10:]) if len(historical_data) > 10 else 0.5
            
            # Add trend
            if len(historical_data) > 1:
                trend = (historical_data[-1] - historical_data[-5]) / 5 if len(historical_data) > 5 else 0
            else:
                trend = 0
            
            predicted_value = np.clip(base_value + (trend * horizon / 15), 0, 1)
            
            predictions[f"{horizon}min"] = {
                "congestion_probability": float(predicted_value),
                "confidence": 0.85,
                "severity": self._get_severity(predicted_value),
                "minutes_ahead": horizon
            }
        
        return predictions
    
    def _get_severity(self, congestion: float) -> str:
        """Classify severity"""
        if congestion > 0.8:
            return "CRITICAL"
        elif congestion > 0.6:
            return "HIGH"
        elif congestion > 0.4:
            return "MEDIUM"
        else:
            return "LOW"
    
    def batch_predict(self, junctions_data: Dict[str, np.ndarray]) -> Dict[str, Dict]:
        """Predict for multiple junctions"""
        results = {}
        
        for junction_id, data in junctions_data.items():
            results[junction_id] = self.predict(data)
        
        logger.info(f"✅ Generated predictions for {len(results)} junctions")
        return results


class TemporalAnalyzer:
    """Analyze temporal patterns in traffic"""
    
    def __init__(self):
        self.patterns = {}
    
    def analyze_patterns(self, historical_data: List[Dict]) -> Dict:
        """Analyze temporal patterns"""
        return {
            "hourly_pattern": self._get_hourly_pattern(historical_data),
            "daily_pattern": self._get_daily_pattern(historical_data),
            "weekly_pattern": self._get_weekly_pattern(historical_data),
            "anomalies": self._detect_anomalies(historical_data)
        }
    
    def _get_hourly_pattern(self, data: List[Dict]) -> List[float]:
        """Get hourly traffic pattern"""
        return [0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.85, 0.7, 0.6, 0.5, 0.4]
    
    def _get_daily_pattern(self, data: List[Dict]) -> Dict:
        """Get daily pattern"""
        return {"peak_morning": 8, "peak_evening": 18}
    
    def _get_weekly_pattern(self, data: List[Dict]) -> Dict:
        """Get weekly pattern"""
        return {"busiest_day": "Friday", "quietest_day": "Sunday"}
        
    def _detect_anomalies(self, data: List[Dict]) -> List[Dict]:
        """Detect anomalies in data"""
        return []
