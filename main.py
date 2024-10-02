import pandas as pd
import matplotlib.pyplot as plt
import random
import os
from matplotlib.ticker import FuncFormatter

if __name__ != "__main__":
    print("not as main")

IN_SECOND = 10e9

subfolders = [f.name for f in os.scandir("io") if f.is_dir()]
types = ["Mark", "Receiver", "Sender", "Status"]
header = [
    "i",
    "iter_value",
    "iter_time",
    "stream_value",
    "stream_time",
    "collector_value",
    "collector_time",
]
iter_header = ["iter_value", "iter_time"]
iter_columns = {"iter_value": "value", "iter_time": "time"}
stream_header = ["stream_value", "stream_time"]
stream_columns = {"stream_value": "value", "stream_time": "time"}
collector_header = ["collector_value", "collector_time"]
collector_columns = {"collector_value": "value", "collector_time": "time"}


# generate stats for each batch
for batch in subfolders:
    for index in types:
        fname = f"io/{batch}/{index}"
        try:
            df = pd.read_csv(
                fname,
                sep=",",
                header=None,
                names=header,
            )

            df = df.drop(
                df[
                    (df.iter_time == 0)
                    | (df.stream_time == 0)
                    | (df.collector_time == 0)
                ].index
            )
            df_index = df.index

            df_iter = df[iter_header].rename(
                columns=iter_columns,
            )
            df_stream = df[stream_header].rename(
                columns=stream_columns,
            )
            df_collector = df[collector_header].rename(
                columns=collector_columns,
            )

            fig, ax = plt.subplots()
            ax.plot(df_index, df_iter["time"] / IN_SECOND, "r-", label="iter")
            ax.plot(df_index, df_stream["time"] / IN_SECOND, "g-", label="stream")
            ax.plot(df_index, df_collector["time"] / IN_SECOND, "b-", label="collector")
            ax.set_ylabel("time, s")
            ax.set_xlabel("iteration")
            ax.legend(loc="upper left")
            ax.set_title(f"{index} {batch} - Time")
            fig.savefig(f"io/{batch}/{index}_{batch}_time.png", bbox_inches="tight")
            fig.clear()

            fig, ax = plt.subplots()
            ax.plot(df_index, df_iter["value"], "r-", label="iter")
            ax.plot(df_index, df_stream["value"], "g-", label="stream")
            ax.plot(df_index, df_collector["value"], "b-", label="collector")
            ax.set_ylabel("value")
            ax.set_xlabel("iteration")
            ax.legend(loc="upper left")
            ax.set_title(f"{index} {batch} - Value")
            fig.savefig(f"io/{batch}/{index}_{batch}_value.png", bbox_inches="tight")
            fig.clear()

            fig, ax = plt.subplots()

            df_index_0_iter = df_index.map(lambda _: random.random())
            df_index_0_stream = df_index.map(lambda _: random.random())
            df_index_0_collector = df_index.map(lambda _: random.random())

            ax.plot(df_index_0_iter, df_iter["time"] / IN_SECOND, "ro", label="iter")
            ax.plot(
                df_index_0_stream, df_stream["time"] / IN_SECOND, "go", label="stream"
            )
            ax.plot(
                df_index_0_collector,
                df_collector["time"] / IN_SECOND,
                "bo",
                label="collector",
            )
            ax.set_ylabel("time, s")
            ax.set_xlim((-2.5, 3.5))
            ax.legend(loc="lower right")
            ax.set_title(f"{index} {batch} - Time bubbles")
            ax.tick_params(labelbottom=False, bottom=False)
            fig.savefig(
                f"io/{batch}/{index}_{batch}_time_bubbles.png", bbox_inches="tight"
            )
            fig.clear()
        except FileNotFoundError:
            print(f"skipping {fname}")
        finally:
            plt.close()


# generate avg for each subfolder
# X - batch size, Y - time
for index in types:
    x = []
    y_iter = []
    y_stream = []
    y_collector = []
    for batch in subfolders:
        fname = f"io/{batch}/{index}"
        try:
            df = pd.read_csv(
                fname,
                sep=",",
                header=None,
                names=header,
            )

            df_index = df.index

            df_iter = df[iter_header].rename(
                columns=iter_columns,
            )
            df_stream = df[stream_header].rename(
                columns=stream_columns,
            )
            df_collector = df[collector_header].rename(
                columns=collector_columns,
            )

            x.append(int(batch))
            y_iter.append(df_iter["time"].mean() / IN_SECOND)
            y_stream.append(df_stream["time"].mean() / IN_SECOND)
            y_collector.append(df_collector["time"].mean() / IN_SECOND)
        except FileNotFoundError:
            print(f"skipping {fname}")

    fig, ax = plt.subplots()
    ax.plot(x, y_iter, "r-", label="iter")
    ax.plot(x, y_stream, "g-", label="stream")
    ax.plot(x, y_collector, "b-", label="collector")
    ax.set_ylabel("time, s")
    ax.set_xlabel("batch size")
    ax.legend(loc="upper left")
    ax.set_title(f"{index} - Avg value")
    fig.savefig(f"io/{index}_avg_value.png", bbox_inches="tight")
    fig.clear()
    plt.close()
