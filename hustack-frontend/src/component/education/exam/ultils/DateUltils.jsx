import {addZeroBefore} from "../../../../utils/dateutils";

export function formatDateTime(value){
  if(value != null && value !== ''){
    let date = new Date(value);
    return (
      addZeroBefore(date.getDate(), 2) +
      "/" +
      addZeroBefore(date.getMonth() + 1, 2) +
      "/" +
      date.getFullYear() +
      " " +
      addZeroBefore(date.getHours(), 2) +
      ":" +
      addZeroBefore(date.getMinutes(), 2) +
      ":" +
      addZeroBefore(date.getSeconds(), 2)
    );
  }
  return null
}

export function formatDate(value){
  if(value != null && value !== ''){
    let date = new Date(value);
    return (
      addZeroBefore(date.getDate(), 2) +
      "/" +
      addZeroBefore(date.getMonth() + 1, 2) +
      "/" +
      date.getFullYear()
    );
  }
  return null
}

export function formatTimeToMMSS(seconds){
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

export function formatDateApi(value) {
  if(value != null && value !== ''){
    let date = new Date(value);
    return (
      date.getFullYear() +
      "-" +
      addZeroBefore(date.getMonth() + 1, 2) +
      "-" +
      addZeroBefore(date.getDate(), 2)
    );
  }
  return null
}

export function formatDateTimeApi(value){
  if(value != null && value !== ''){
    let date = new Date(value);
    return (
      date.getFullYear() +
      "-" +
      addZeroBefore(date.getMonth() + 1, 2) +
      "-" +
      addZeroBefore(date.getDate(), 2)+
      " " +
      addZeroBefore(date.getHours(), 2) +
      ":" +
      addZeroBefore(date.getMinutes(), 2) +
      ":" +
      addZeroBefore(date.getSeconds(), 2)
    );
  }
  return null
}

export function getDiffMinutes(date1, date2){
  if(date1 != null && date1 !== '' && date2 != null && date2 !== ''){
    const valueDate1 = new Date(date1);
    const valueDate2 = new Date(date2);
    const diffMs = valueDate2.getTime() - valueDate1.getTime();
    return Math.floor(diffMs / 60000)
  }
  return null;
}


export function getDiffSeconds(date1, date2){
  if(date1 != null && date1 !== '' && date2 != null && date2 !== ''){
    const valueDate1 = new Date(date1);
    const valueDate2 = new Date(date2);
    const diffMs = valueDate2.getTime() - valueDate1.getTime();
    return Math.floor(diffMs / 1000)
  }
  return null;
}
