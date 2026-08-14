# TaskGenerator — генератор синтетических данных экспериментов

Скрипт генерирует наборы данных (задачи и их профили запросов) для экспериментов по
адаптивному планированию нагрузки. На основе `config.json` строится иерархия каталогов
со сценариями и прогонами, в каждом из которых создаётся `dataset.csv`.

## Требования

- Python 3
- numpy
- pandas

## Запуск

    python task_generator.py [путь/к/config.json]

Если путь не указан, используется `experiments/config.json`.

## Формат config.json

```json
{
  "requests-per-minute": 600,
  "amount-tasks": 200,
  "seed": 42,
  "experiments": [
    {
      "distribution": {
        "requests-in-task-amount": {
          "type": "lognorm",
          "params": {
            "mu": 1.5,
            "sigma": 0.5
          }
        },
        "request-duration": {
          "type": "exp",
          "params": {
            "mean": 0.2
          }
        }
      },
      "scenarios": {
        "arrival-interval": [
          1.0,
          0.5,
          0.2
        ],
        "runs": 5
      }
    }
  ]
}
```

Поля:
- requests-per-minute — значение RPM, сохраняется в meta.json каждого сценария (см. раздел «Важно»).
- amount-tasks — число задач в одном прогоне.
- seed — базовый seed (необязательно, по умолчанию 42).
- experiments — список экспериментов, каждый со своим распределением и набором сценариев.
- distribution.requests-in-task-amount — распределение числа запросов в задаче.
- distribution.request-duration — распределение длительности одного запроса.
- scenarios.arrival-interval — список интервалов прихода задач (сек), каждый интервал = отдельный сценарий.
- scenarios.runs — число повторных прогонов на сценарий (по умолчанию 5).

## Поддерживаемые распределения

requests-in-task-amount (число запросов в задаче):
- lognorm — параметры mu, sigma, np.random.lognormal, результат округляется, минимум 1.
- normal — параметры mean, std, np.random.normal, округляется, минимум 1.
- любой другой type — НЕ является случайным: возвращается int(params["mean"]) детерминированно (в params обязателен ключ mean).

request-duration (длительность одного запроса):
- exp — параметр mean, np.random.exponential(scale=mean).
- lognorm — параметры mu, sigma.
- normal — параметры mean, std.
- любой другой type — бросает ValueError (в отличие от распределения числа запросов).

Длительность всегда ограничена снизу значением 0.1 (сек).

## Структура вывода

```
<base_dir>/
    dist_<тип>_<параметры>/               
        scenario_001/                    
            meta.json   
            run_0/
                dataset.csv
            run_1/
                dataset.csv
            ...
        scenario_002/
        ...

```
- base_dir — каталог, где лежит config.json (или experiments, если путь без директории).
- Имя папки распределения:
    - dist_lognorm_{sigma}_{mu} (именно в таком порядке — сигма перед мю)
    - dist_normal_{mean}_{std}
    - dist_{type} для прочих типов
- dataset.csv содержит два столбца:
    - arrival_time — момент прихода задачи (равномерно: 0, interval, 2*interval, ...)
    - profile — строковое представление Python-списка длительностей запросов задачи (str(list)), не JSON и не отдельные столбцы — для разбора нужен ast.literal_eval или аналог.
- meta.json содержит: оба распределения, arrival-interval сценария и rpm.

## Seed

seed = config.get("seed", 42) + run_id + idx * 100

где idx — порядковый номер интервала в arrival-interval (начиная с 1), run_id — номер прогона (начиная с 0).
