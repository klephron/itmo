import { createEffect, createSignal, onMount } from "solid-js";

// NOTE: set canvas width and height same as width and height of elements
type Props = {
  width: number;
  height: number;
};

function Map(props: Props) {
  const zoomFactor = 1.1;
  const zoomMin = 0.2;
  const zoomMax = 5;
  const fpsMax = 60;

  let coordinateX = 0;
  let coordinateY = 0;
  let zoom = 1;
  let fps = 0;
  let drawTimeMs = 0;
  let play = true;

  let isDragging = false;
  let dragStartClientX = 0;
  let dragStartClientY = 0;
  let dragStartCoordinateX = coordinateX;
  let dragStartCoordinateY = coordinateY;

  let canvas: HTMLCanvasElement;
  let ctx: CanvasRenderingContext2D | null;

  let objects = generateObjects(1000)

  function generateObjects(cnt: number) {
    function getRandomInt(max: number) {
      return Math.floor(Math.random() * max);
    }

    const colors = ["red", "green", "blue", "yellow", "white", "purple"];
    const objs = [];

    for (let i = 0; i < cnt; ++i) {
      const obj = {
        "x": getRandomInt(10000),
        "y": getRandomInt(10000),
        "color": colors[getRandomInt(colors.length)]
      }
      objs.push(obj)
    }
    return objs;
  }

  onMount(() => {
    ctx = canvas.getContext("2d");
    if (ctx == null) {
      alert("2d is not supported");
      return;
    }

    canvasPlayScene(ctx);
  });

  function onMouseDown(e: MouseEvent) {
    // Middle mouse
    if (e.button === 1) {
      isDragging = true;
      dragStartClientX = e.clientX;
      dragStartClientY = e.clientY;
      dragStartCoordinateX = coordinateX;
      dragStartCoordinateY = coordinateY;
    }
  }

  function onMouseMove(e: MouseEvent) {
    if (isDragging) {
      let dragX = (e.clientX - dragStartClientX) / zoom;
      let dragY = (e.clientY - dragStartClientY) / zoom;

      coordinateX = dragStartCoordinateX - dragX;
      coordinateY = dragStartCoordinateY - dragY;
    }
  }

  function onMouseUp() {
    if (isDragging) {
      isDragging = false;
    }
  }

  function onWheel(e: WheelEvent) {
    e.preventDefault();

    const mouseX = e.clientX - canvas.offsetLeft;
    const mouseY = e.clientY - canvas.offsetTop;

    const zoomBefore = zoom;

    if (e.deltaY < 0) {
      zoom = Math.min(zoom * zoomFactor, zoomMax);
    } else {
      zoom = Math.max(zoom / zoomFactor, zoomMin);
    }

    if (e.deltaY > 0 || e.deltaY < 0) {
      const diffX = mouseX / zoomBefore - mouseX / zoom;
      const diffY = mouseY / zoomBefore - mouseY / zoom;

      coordinateX += diffX;
      coordinateY += diffY;
    }
  }

  function canvasClear(ctx: CanvasRenderingContext2D) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }

  function canvasDrawGrid(ctx: CanvasRenderingContext2D) {
    const width = canvas.width;
    const height = canvas.height;
    const gridWidth = 50;
    const gridHeight = 50;

    const offsetX = coordinateX % gridWidth;
    const offsetY = coordinateY % gridHeight;

    ctx.save();
    ctx.scale(zoom, zoom);

    ctx.beginPath();
    for (let x = -gridWidth; x < (width + gridWidth) / zoom; x += gridWidth) {
      ctx.moveTo(x - offsetX, -gridHeight);
      ctx.lineTo(x - offsetX, (height + gridHeight) / zoom);
    }
    for (
      let y = -gridHeight;
      y < (height + gridHeight) / zoom;
      y += gridHeight
    ) {
      ctx.moveTo(-gridWidth, y - offsetY);
      ctx.lineTo((width + gridWidth) / zoom, y - offsetY);
    }
    ctx.strokeStyle = "#ccc";

    ctx.stroke();
    ctx.restore();
  }

  function canvasDrawCoordinates(ctx: CanvasRenderingContext2D) {
    const infoWidth = 250;
    const infoHeight = 30;

    let width = canvas.width;

    ctx.fillStyle = "gray";
    ctx.fillRect(width - infoWidth, 0, infoWidth, infoHeight);

    ctx.font = "12px Arial";
    ctx.fillStyle = "white";
    const text =
      "X: " +
      coordinateX.toFixed(0) +
      ", Y: " +
      coordinateY.toFixed(0) +
      ", Z: " +
      zoom.toFixed(2) +
      ", fps: " +
      fps.toFixed(0) +
      ", ms: " +
      drawTimeMs.toFixed(0);

    const textM = ctx.measureText(text);

    const textX = width - infoWidth + (infoWidth - textM.width) / 2;
    const textY = (infoHeight + 7) / 2;

    ctx.fillText(text, textX, textY);
  }

  function canvasDrawObjects(ctx: CanvasRenderingContext2D) {
    const radius = 20;

    ctx.save();
    ctx.scale(zoom, zoom);
    ctx.translate(-coordinateX, -coordinateY);

    for (const object of objects) {
      ctx.beginPath();
      ctx.arc(object.x, object.y, radius, 0, 2 * Math.PI, false);
      ctx.fillStyle = object.color;
      ctx.fill();
      ctx.strokeStyle = "#003300";
      ctx.stroke();
    }

    ctx.restore();
  }

  function canvasDrawScene(ctx: CanvasRenderingContext2D) {
    canvasClear(ctx);
    canvasDrawGrid(ctx);
    canvasDrawObjects(ctx);
    canvasDrawCoordinates(ctx);
  }

  async function canvasPlayScene(ctx: CanvasRenderingContext2D) {
    const frameMsMin = 1_000 / fpsMax;

    function sleep(ms: number) {
      return new Promise((r) => setTimeout(r, ms));
    }

    function getUnixMsNow() {
      return new Date().getTime();
    }

    while (play) {
      const startTimeMs = getUnixMsNow();

      canvasDrawScene(ctx);

      const timeMs = getUnixMsNow() - startTimeMs;
      const timeLimitedMs = Math.max(frameMsMin, timeMs);

      drawTimeMs = timeMs;
      fps = 1_000 / timeLimitedMs; // NOTE: actual fps may differ

      const waitTimeMs = timeLimitedMs - timeMs;
      await sleep(waitTimeMs);
    }
  }

  return (
    <canvas
      ref={canvas!}
      style={{
        border: "1px solid grey",
        "border-radius": "2px",
        width: props.width + "px",
        height: props.height + "px",
      }}
      width={props.width}
      height={props.height}
      onMouseDown={onMouseDown}
      onMouseUp={onMouseUp}
      onMouseMove={onMouseMove}
      onWheel={onWheel}
      onContextMenu={(e) => e.preventDefault()}
    ></canvas>
  );
}

export default Map;
