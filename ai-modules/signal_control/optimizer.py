"""
Adaptive Signal Control Module - PPO Reinforcement Learning
"""

import numpy as np
from typing import Dict, Tuple
import logging

logger = logging.getLogger(__name__)
     
               
class SignalOptimizer:
    """Optimize traffic signal timing using PPO"""
      
    def __init__(self):
        self.base_cycle = 90  # seconds
        self.min_green = 15
        self.max_green = 80
        logger.info("✅ Signal optimizer initialized")
    
    def optimize(self, junction_state: Dict) -> Dict:
        """
        Optimize signal timing for a junction
        
        Args:
            junction_state: Current traffic state
        
        Returns:
            Optimized signal timing
        """
        vehicle_count = junction_state.get("vehicle_count", 0)
        queue_length = junction_state.get("queue_length", 0)
        current_state = junction_state.get("current_state", "red")
        
        # PPO-based optimization (simplified)
        # In production: use trained PPO model
        green_time = self._calculate_green_time(vehicle_count, queue_length)
     
        return {
            "green_duration": green_time,
            "yellow_duration": 5,
            "red_duration": self.base_cycle - green_time - 5,
            "cycle_length": self.base_cycle,
            "optimization_score": self._calculate_score(vehicle_count)
        }
    
    def _calculate_green_time(self, vehicles: int, queue: float) -> int:
        """Calculate optimal green time"""
        # Simplified calculation
        if queue > 0.8:
            return self.max_green
        elif queue > 0.6:
            return self.min_green + int((self.max_green - self.min_green) * 0.7)
        elif queue > 0.4:
            return self.min_green + int((self.max_green - self.min_green) * 0.5)
        else:
            return self.min_green
    
    def _calculate_score(self, vehicles: int) -> float:
        """Calculate optimization score"""
        return min(1.0, vehicles / 150)  # Normalized score
    
    def batch_optimize(self, junctions: Dict[str, Dict]) -> Dict[str, Dict]:
        """Optimize all junctions"""
        optimized = {}
        
        for junction_id, state in junctions.items():
            optimized[junction_id] = self.optimize(state)
             
        logger.info(f"✅ Optimized {len(optimized)} junctions")
        return optimized


class SignalCoordinator:
    """Coordinate signals for smooth traffic flow"""
    
    def __init__(self):
        self.offset_map = {}
    
    def coordinate_signals(self, junctions: List[str], positions: Dict[str, Tuple]) -> Dict:
        """Coordinate signal offsets"""
        offsets = {}
        
        for i, junction in enumerate(junctions):
            # Calculate offset to create green wave
            offsets[junction] = (i * 15) % 90
               
        return offsets
