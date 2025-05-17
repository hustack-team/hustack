import {config} from "../../../../config/config";

export const prefixFile = config.url.API_URL + '/service/files/'
export function getFilePathFromString(value){
  return prefixFile + value
}

export function getFileIdFromString(value){
  const files = value.split('/')
  return files[0]
}

export function getFilenameFromString(value){
  const files = value.split('/')
  return files[files.length - 1]
}

export function getFilenameFromPath(value){
  const files = value.split('/')
  return files[files.length - 2]
}

export function getFileFromListFileAndFileAnswerAndExamResultDetailsId(list, fileAnswer, examResultDetailsId){
  for(let item of list){
    const fileName = item.name.split('.');
    const subFileParts = fileName[0].split('_')
    if(subFileParts[1] === examResultDetailsId && subFileParts[subFileParts.length-1] === getFileIdFromString(fileAnswer)){
      return item
    }
  }
  return null
}

export function getFileCommentFromFileAnswerAndExamResultDetailsId(filePathComment, fileAnswer, examResultDetailsId){
  const files = filePathComment.split(';')
  for(let file of files){
    const filename = getFilenameFromString(file)
    const subFilenames = filename.split('.')
    const subFileParts = subFilenames[0].split('_')
    if(subFileParts[1] === examResultDetailsId && subFileParts[subFileParts.length-1] === getFileIdFromString(fileAnswer)){
      return file
    }
  }
  return null
}

export function getFilenameFromFileNew(files){
  let res = []
  for(let file of files){
    res.push(file.name)
  }
  return res.join(";")
}

export function checkFilePdf(file){
  if(file?.toLowerCase().includes(".pdf")){
    return true
  }
  return false
}

export function checkFileImage(file){
  if(file?.toLowerCase().includes(".png") ||
    file?.toLowerCase().includes(".jpg")
  ){
    return true
  }
  return false
}

// Nén ảnh
export function compressImage(file, maxWidth, maxHeight, maxSizeMB) {
  const targetSizeBytes = maxSizeMB * 1024 * 1024;
  return new Promise((resolve, reject) => {
    if (file.size <= targetSizeBytes) {
      resolve(file);
      return;
    }

    const img = new Image();
    const reader = new FileReader();

    reader.onload = (e) => {
      img.src = e.target.result;
    };

    img.onload = () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');

      let width = img.width;
      let height = img.height;

      // Giữ tỉ lệ ảnh
      if (width > height) {
        if (width > maxWidth) {
          height = Math.round((height * maxWidth) / width);
          width = maxWidth;
        }
      } else {
        if (height > maxHeight) {
          width = Math.round((width * maxHeight) / height);
          height = maxHeight;
        }
      }

      canvas.width = width;
      canvas.height = height;
      ctx.drawImage(img, 0, 0, width, height);

      let quality = 0.9;
      const tryCompress = () => {
        canvas.toBlob(
          (blob) => {
            if (blob.size <= targetSizeBytes || quality <= 0.1) {
              const compressedFile = new File([blob], file.name, {
                type: file.type,
                lastModified: file.lastModified,
              });
              resolve(compressedFile);
            } else {
              quality -= 0.05;
              tryCompress();
            }
          },
          file.type,
          quality
        );
      };

      tryCompress();
    };

    img.onerror = reject;
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}
