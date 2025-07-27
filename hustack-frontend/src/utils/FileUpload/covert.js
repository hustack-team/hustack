export async function dataUrlToFile(dataUrl, fileName) {
  const res = await fetch(dataUrl);
  const blob = await res.blob();
  return new File([blob], fileName, { type: "image/png" });
}

export const randomImageName = () => Math.random().toString(36).substr(2, 5);

const IMAGE_EXTENSION = [".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".svg"];
const PDF_EXTENSION = [".pdf"];
const WORD_EXTENSION = [".doc", ".docx", ".docm"];
const EXCEL_EXTENSION = [".xls", ".xlsx", ".xlsm"];
const TEXT_EXTENSION = [".txt", ".md", ".log"];
const CODE_EXTENSION = [".js", ".jsx", ".ts", ".tsx", ".java", ".py", ".cpp", ".c", ".cs", ".php", ".html", ".css", ".scss", ".json", ".xml", ".sql", ".sh", ".bat", ".ps1"];
const VIDEO_EXTENSION = [".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mkv", ".m4v"];
const AUDIO_EXTENSION = [".mp3", ".wav", ".flac", ".aac", ".ogg", ".wma", ".m4a"];
const ARCHIVE_EXTENSION = [".zip", ".rar", ".7z", ".tar", ".gz", ".bz2"];

export const getFileType = (fileName) => {
  if (!fileName) {
    return "unknown";
  }
  
  const fileNameLowerCase = fileName.toString().toLowerCase();
  
  // Kiểm tra từng loại extension
  for (const extension of IMAGE_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "img";
  }
  for (const extension of PDF_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "pdf";
  }
  for (const extension of WORD_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "word";
  }
  for (const extension of EXCEL_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "excel";
  }
  for (const extension of TEXT_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "txt";
  }
  for (const extension of CODE_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "code";
  }
  for (const extension of VIDEO_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "video";
  }
  for (const extension of AUDIO_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "audio";
  }
  for (const extension of ARCHIVE_EXTENSION) {
    if (fileNameLowerCase.endsWith(extension)) return "archive";
  }
  
  // Fallback cho các loại file không xác định
  return "unknown";
};

function base64ToArrayBuffer(base64) {
  var binaryString = window.atob(base64);
  var binaryLen = binaryString.length;
  var bytes = new Uint8Array(binaryLen);
  for (var i = 0; i < binaryLen; i++) {
    var ascii = binaryString.charCodeAt(i);
    bytes[i] = ascii;
  }
  return bytes;
}

export const saveByteArray = (fileName, byte, fileType) => {
  let blobType = "";
  switch (fileType) {
    case "pdf":
      blobType = "application/pdf"
      break;
    case "word":
      blobType = "application/msword"
      break;
    case "excel":
      blobType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      break;
    case "txt":
      blobType = "text/plain"
      break;
    case "code":
      blobType = "text/plain"
      break;
    case "img":
      blobType = "image/*"
      break;
    case "video":
      blobType = "video/*"
      break;
    case "audio":
      blobType = "audio/*"
      break;
    case "archive":
      blobType = "application/zip"
      break;
    default:
      blobType = "application/octet-stream"
      break;
  }

  var bytes = base64ToArrayBuffer(byte);
  var blob = new Blob([bytes], { type: blobType });
  var link = document.createElement("a");
  link.href = window.URL.createObjectURL(blob);
  link.download = fileName;
  link.click();
}