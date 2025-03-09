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
