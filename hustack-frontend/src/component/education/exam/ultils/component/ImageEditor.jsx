import React, {useState, useRef, useEffect} from "react";
import { makeStyles } from "@material-ui/core/styles";
import { ZoomIn, ZoomOut, Delete } from "@mui/icons-material";
import { CircularProgress } from "@mui/material";
import {getFilenameFromPath} from "../FileUltils";

const useStyles = makeStyles((theme) => ({
  filePreview: {
    border: '1px solid black',
    borderRadius: '8px'
  },
}));

export default function ImageEditor(props) {
  const classes = useStyles();
  const {
    file,
    examResultDetailsIdSelected,
    indexSelected,
    isEdit,
    isSave,
    setImageComment,
    setContentComment,
  } = props

  const [widthImage, setWidthImage] = useState(600)
  const [isLoadingImage, setIsLoadingImage] = useState(true);

  const canvasRef = useRef(null);
  const ctxRef = useRef(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [widthCanvas, setWidthCanvas] = useState(600)
  const [isLoadingCanvas, setIsLoadingCanvas] = useState(true);
  const [texts, setTexts] = useState([]);
  const [dragging, setDragging] = useState(null);

  useEffect(() => {
    if (!isEdit) return;

    const canvas = canvasRef.current;
    if(!canvas) return

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.src = file;
    img.onload = () => {
      // canvas.width = img.naturalWidth;
      // canvas.height = img.naturalHeight;
      const aspectRatio = img.naturalWidth / img.naturalHeight;

      canvas.width = widthCanvas;
      canvas.height = canvas.width / aspectRatio;

      const ctx = canvas.getContext('2d');
      ctxRef.current = ctx;

      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      setIsLoadingCanvas(false);
    };
  }, [isEdit, widthCanvas]);

  useEffect(() => {
    if(isSave){
      saveImage()
      const text = texts.map(t => `<p>${t.text}</p>`).join('');
      setContentComment(`<div><strong>Câu ${indexSelected+1}: </strong> ${text}</div>`)
    }
  }, [isSave]);

  const startDrawing = (e) => {
    ctxRef.current.beginPath();
    ctxRef.current.moveTo(e.nativeEvent.offsetX, e.nativeEvent.offsetY);
    setIsDrawing(true);
  };

  const draw = (e) => {
    if (!isDrawing) return;
    ctxRef.current.lineTo(e.nativeEvent.offsetX, e.nativeEvent.offsetY);
    ctxRef.current.strokeStyle = 'red';
    ctxRef.current.lineWidth = 3;
    ctxRef.current.stroke();
  };

  const stopDrawing = () => {
    ctxRef.current.closePath();
    setIsDrawing(false);
  };

  const handleDoubleClick = (e) => {
    const x = e.nativeEvent.offsetX;
    const y = e.nativeEvent.offsetY;
    setTexts([...texts, { id: Date.now(), x, y, text: '' }]);
  };

  const updateText = (id, newText) => {
    setTexts(texts.map((t) => (t.id === id ? { ...t, text: newText } : t)));
  };

  const removeText = (id) => {
    setTexts(texts.filter((t) => t.id !== id));
  };

  const addTextToCanvas = () => {
    const ctx = ctxRef.current;
    ctx.font = '16px Arial';
    ctx.fillStyle = 'red';
    const lineHeight = 16;
    const maxWidth = 200;

    texts.forEach(({ x, y, text }) => {
      const lines = text.split("\n");

      let wrappedLines = [];
      lines.forEach(line => {
        let words = line.split(" ");
        let currentLine = "";

        words.forEach((word) => {
          const testLine = currentLine ? currentLine + " " + word : word;
          const testWidth = ctx.measureText(testLine).width;

          if (testWidth > maxWidth) {
            wrappedLines.push(currentLine);
            currentLine = word;
          } else {
            currentLine = testLine;
          }
        });

        wrappedLines.push(currentLine);
      });

      wrappedLines.forEach((line, index) => {
        ctx.fillText(line, x, y + index * lineHeight);
      });
    });
  };

  const saveImage = () => {
    addTextToCanvas();
    // console.log(canvasRef.current.toDataURL('image/png'));
    canvasRef.current.toBlob((blob) => {
      if (blob) {
        const fileRes = new File([blob], `comment_${examResultDetailsIdSelected}_${getFilenameFromPath(file)}.png`, { type: "image/png" });
        setImageComment(fileRes)
      }
    }, "image/png");
    // const link = document.createElement("a");
    // link.href = canvasRef.current.toDataURL("image/png");
    // link.download = "edited-image.png";
    // link.click();
  };

  const startDrag = (id, e) => {
    e.stopPropagation();
    const textItem = texts.find((t) => t.id === id);
    setDragging({
      id,
      offsetX: e.nativeEvent.pageX - textItem.x,
      offsetY: e.nativeEvent.pageY - textItem.y,
    });
  };

  const onDrag = (e) => {
    if (!dragging) return;
    setTexts((texts) =>
      texts.map((t) =>
        t.id === dragging.id
          ? {
            ...t,
            x: e.nativeEvent.pageX - dragging.offsetX,
            y: e.nativeEvent.pageY - dragging.offsetY,
          }
          : t
      )
    );
  };

  const stopDrag = () => {
    setDragging(null);
  };

  const zoomCanvas = (isZoomIn) => {
    setWidthCanvas(isZoomIn ? widthCanvas + 50 : widthCanvas - 50)
    setIsLoadingCanvas(true)
  }

  return (
    <div style={{textAlign: "center"}}>
      {
        !isEdit && !isLoadingImage && (
          <div style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: "10px",
            marginBottom: "10px",
            position: "sticky",
            top: "-15px"
          }}>
            <button style={{border: "none", borderRadius: "10px", cursor: "pointer"}}>
              <ZoomOut
                onClick={() => setWidthImage(widthImage - 50)}
              />
            </button>
            <button style={{border: "none", borderRadius: "10px", cursor: "pointer"}}>
              <ZoomIn
                onClick={() => setWidthImage(widthImage + 50)}
              />
            </button>
          </div>
        )
      }
      {
        isEdit && !isLoadingCanvas && (
          <div>
            <div style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: "10px"
            }}>
              <button style={{border: "none", borderRadius: "10px", cursor: "pointer"}}>
                <ZoomOut
                  onClick={() => zoomCanvas(false)}
                />
              </button>
              <button style={{border: "none", borderRadius: "10px", cursor: "pointer"}}>
                <ZoomIn
                  onClick={() => zoomCanvas(true)}
                />
              </button>
            </div>
            <h4 style={{color: "red", margin: "5px 0"}}>*Lưu ý: ZoomIn/Out ảnh có thể làm xô lệch vị trí text-note và mất đường vẽ!</h4>
          </div>
        )
      }
      {
        !isEdit ? (
          <div style={{width: "100%", height: "100%"}}>
            {isLoadingImage && (
              <div
                style={{
                  position: "absolute",
                  top: "50%",
                  left: "50%",
                  transform: "translate(-50%, -50%)",
                }}
              >
                <CircularProgress size={40}/>
              </div>
            )}
            <img
              src={file}
              alt="Image"
              className={!isLoadingImage && classes.filePreview}
              style={{width: widthImage + 'px'}}
              onLoad={() => setIsLoadingImage(false)}
            />
          </div>
        ) : (
          <div
            style={{position: 'relative', display: 'inline-block'}}
            onMouseMove={onDrag}
            onMouseUp={stopDrag}
          >
            {isLoadingCanvas && (
              <div
                style={{
                  position: "absolute",
                  top: "10px",
                  left: "50%",
                  transform: "translate(-50%, -50%)",
                }}
              >
                <CircularProgress size={40}/>
              </div>
            )}
            <canvas
              ref={canvasRef}
              onMouseDown={startDrawing}
              onMouseMove={draw}
              onMouseUp={stopDrawing}
              onDoubleClick={handleDoubleClick}
              style={!isLoadingCanvas ? { border: "1px solid black", cursor: "crosshair", borderRadius: "8px" } : {}}
            />
            {texts.map(({id, x, y, text}) => (
              <div
                key={id}
                onMouseDown={(e) => startDrag(id, e)}
                style={{
                  position: 'absolute',
                  top: y,
                  left: x,
                  cursor: 'move',
                  display: 'flex',
                  alignItems: 'center',
                  padding: '2px',
                  borderRadius: '4px',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.querySelector('button').style.display =
                    'inline-block';
                  e.currentTarget.querySelector('#image-comment-content').style.border =
                    '1px solid black';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.querySelector('button').style.display = 'none';
                  e.currentTarget.querySelector('#image-comment-content').style.border = 'none';
                }}
              >
                <textarea
                  value={text}
                  onChange={(e) => updateText(id, e.target.value)}
                  id="image-comment-content"
                  style={{
                    fontSize: '16px',
                    color: 'red',
                    padding: '2px',
                    border: 'none',
                    outline: 'none',
                    background: 'transparent',
                    fieldSizing: 'content',
                    resize: 'none',
                    width: '200px',
                    position: 'relative'
                  }}
                ></textarea>
                <button
                  onClick={() => removeText(id)}
                  style={{display: 'none', background: "transparent", outline: "none", border: 'none'}}
                >
                  <Delete
                    style={{cursor: 'pointer', color: 'red'}}
                  />
                </button>
              </div>
            ))}
          </div>
        )
      }
    </div>
  );
}
