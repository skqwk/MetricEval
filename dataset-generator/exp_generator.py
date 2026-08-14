import json
import os
import numpy as np
import pandas as pd
from typing import Dict, Any, List, Optional
import random


class TaskGenerator:
    """
    Генератор заданий с заданными распределениями:
    - числа запросов в задании
    - длительности одного запроса
    """

    def __init__(self,
                 rpm_limit: float,
                 arrival_interval: float,
                 task_count: int,
                 req_dist: Dict[str, Any],  # {"type": "lognorm", "params": {...}}
                 duration_dist: Dict[str, Any],  # {"type": "exp", "params": {...}}
                 seed: Optional[int] = None):

        self.rpm_limit = rpm_limit
        self.arrival_interval = arrival_interval
        self.task_count = task_count
        self.req_dist = req_dist
        self.duration_dist = duration_dist
        self.seed = seed

        if seed is not None:
            np.random.seed(seed)
            random.seed(seed)

    def generate_arrival_times(self) -> List[float]:
        """Равномерное поступление задач"""
        times = [0.0]
        for i in range(1, self.task_count):
            times.append(times[-1] + self.arrival_interval)
        return times

    def generate_request_count(self) -> int:
        """Число запросов в задаче согласно распределению"""
        dist_type = self.req_dist['type']
        params = self.req_dist.get('params', {})

        if dist_type == 'lognorm':
            mu = params.get('mu')
            sigma = params.get('sigma')
            val = np.random.lognormal(mean=mu, sigma=sigma)
            return int(max(1, round(val)))
        elif dist_type == 'normal':
            mean = params.get('mean')
            std = params.get('std')
            val = np.random.normal(mean, std)
            return int(max(1, round(val)))
        else:
            mean = params.get('mean')
            return int(mean)

    def generate_duration(self) -> float:
        """Длительность одного запроса согласно распределению"""
        dist_type = self.duration_dist['type']
        params = self.duration_dist.get('params', {})

        if dist_type == 'exp':
            mean = params.get('mean')
            dur = np.random.exponential(scale=mean)
        elif dist_type == 'lognorm':
            mu = params.get('mu')
            sigma = params.get('sigma')
            dur = np.random.lognormal(mean=mu, sigma=sigma)
        elif dist_type == 'normal':
            mean = params.get('mean')
            std = params.get('std')
            dur = np.random.normal(mean, std)
        else:
            raise ValueError(f"Неизвестное распределение длительности: {dist_type}")

        return max(0.1, dur)  # минимальная длительность 0.1 сек

    def generate_task(self) -> List[float]:
        """Генерирует одну задачу: список длительностей запросов"""
        request_count = self.generate_request_count()
        durations = [self.generate_duration() for _ in range(request_count)]
        return durations

    def generate_dataset(self) -> pd.DataFrame:
        arrival_times = self.generate_arrival_times()
        profiles = [self.generate_task() for _ in range(self.task_count)]
        df = pd.DataFrame({
            'arrival_time': arrival_times,
            'profile': [str(p) for p in profiles]
        })
        return df

    def save_dataset(self, df: pd.DataFrame, filepath: str):
        df.to_csv(filepath, index=False)
        print(f"  Dataset saved to {filepath}")


def get_distribution_folder_name(req_dist: Dict[str, Any]) -> str:
    """Генерирует имя папки на основе распределения числа запросов"""
    dist_type = req_dist['type']
    params = req_dist.get('params', {})

    if dist_type == 'lognorm':
        mu = params.get('mu')
        sigma = params.get('sigma')
        return f"dist_lognorm_{sigma}_{mu}"
    elif dist_type == 'normal':
        mean = params.get('mean')
        std = params.get('std')
        return f"dist_normal_{mean}_{std}"
    else:
        return f"dist_{dist_type}"


def create_experiment_structure(config_path: str):
    """Главная функция: читает config.json и генерирует всю иерархию"""
    with open(config_path, 'r', encoding='utf-8') as f:
        config = json.load(f)

    rpm = config['requests-per-minute']
    task_count = config['amount-tasks']
    experiments = config['experiments']

    base_dir = os.path.dirname(config_path) or 'experiments'

    for exp in experiments:
        distribution = exp['distribution']
        req_dist = distribution['requests-in-task-amount']
        dur_dist = distribution['request-duration']

        scenarios_conf = exp['scenarios']
        arrival_intervals = scenarios_conf['arrival-interval']
        runs_count = scenarios_conf.get('runs', 5)

        # Папка для данного распределения (по числу запросов)
        dist_folder = get_distribution_folder_name(req_dist)
        dist_path = os.path.join(base_dir, dist_folder)
        os.makedirs(dist_path, exist_ok=True)

        # Для каждого arrival_interval
        for idx, interval in enumerate(arrival_intervals, start=1):
            scenario_folder = f"scenario_{idx:03d}"
            scenario_path = os.path.join(dist_path, scenario_folder)
            os.makedirs(scenario_path, exist_ok=True)

            # meta.json
            meta = {
                "distribution": {
                    "requests-in-task-amount": req_dist,
                    "request-duration": dur_dist
                },
                "arrival-interval": interval,
                "rpm": rpm
            }
            meta_path = os.path.join(scenario_path, "meta.json")
            with open(meta_path, 'w', encoding='utf-8') as f:
                json.dump(meta, f, indent=4)
            print(f"Created {meta_path}")

            # Генерация прогонов
            for run_id in range(runs_count):
                run_folder = f"run_{run_id}"
                run_path = os.path.join(scenario_path, run_folder)
                os.makedirs(run_path, exist_ok=True)

                # Уникальный seed для воспроизводимости
                base_seed = config.get('seed', 42)
                seed = base_seed + run_id + idx * 100

                generator = TaskGenerator(
                    rpm_limit=rpm,
                    arrival_interval=interval,
                    task_count=task_count,
                    req_dist=req_dist,
                    duration_dist=dur_dist,
                    seed=seed
                )
                df = generator.generate_dataset()
                dataset_path = os.path.join(run_path, "dataset.csv")
                generator.save_dataset(df, dataset_path)
                print(f"  Generated run {run_id} for interval {interval} sec")

    print("\n✅ Все эксперименты успешно созданы!")


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        config_file = sys.argv[1]
    else:
        config_file = "experiments/config.json"
    create_experiment_structure(config_file)