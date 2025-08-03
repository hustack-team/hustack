import {Box, IconButton, Tooltip, Typography} from "@mui/material";
import {amber, blue, green, grey, orange, purple, red} from "@mui/material/colors";
import {makeStyles} from "@material-ui/core/styles";
import React from "react";
import DeleteIcon from "@mui/icons-material/Delete";
import DownloadIcon from '@mui/icons-material/Download';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DescriptionIcon from '@mui/icons-material/Description';
import ImageIcon from '@mui/icons-material/Image';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import TableChartIcon from '@mui/icons-material/TableChart';
import CodeIcon from '@mui/icons-material/Code';
import VideoFileIcon from '@mui/icons-material/VideoFile';
import AudioFileIcon from '@mui/icons-material/AudioFile';
import ArchiveIcon from '@mui/icons-material/Archive';
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import {useTranslation} from "react-i18next";
import {getFileType, saveByteArray} from "./covert";

const useStyles = makeStyles((theme) => ({
  fileContainer: {
    marginTop: "12px",
  },
  fileWrapper: {
    position: "relative",
  },
  fileDownload: {
    display: "flex",
    flexDirection: "row",
    marginBottom: "16px",
    alignItems: "center",
  },
  fileName: {
    fontStyle: "italic",
    paddingRight: "12px",
  },
  downloadButton: {
    marginLeft: "12px",
  },
  buttonClearImage: {
    position: "absolute",
    top: "12px",
    right: "12px",
    zIndex: 3,
    color: "red",
    width: 32,
    height: 32,
    cursor: "pointer",
  },
}));

const getFileIcon = (fileName) => {
  const fileType = getFileType(fileName);
  switch (fileType) {
    case 'pdf':
      return <PictureAsPdfIcon sx={{color: red[700]}}/>;
    case 'word':
      return <DescriptionIcon sx={{color: blue[700]}}/>;
    case 'txt':
      return <DescriptionIcon sx={{color: green[700]}}/>;
    case 'img':
      return <ImageIcon sx={{color: orange[700]}}/>;
    case 'excel':
      return <TableChartIcon sx={{color: green[700]}}/>;
    case 'code':
      return <CodeIcon sx={{color: purple[700]}}/>;
    case 'video':
      return <VideoFileIcon sx={{color: amber[700]}}/>;
    case 'audio':
      return <AudioFileIcon sx={{color: green[700]}}/>;
    case 'archive':
      return <ArchiveIcon sx={{color: grey[700]}}/>;
    case 'unknown':
    default:
      return <InsertDriveFileIcon sx={{color: grey[600]}}/>;
  }
};

function FileUploadZone(props) {
  const {file, removable, onRemove, onDownload, downloadable = true} = props;
  const {t} = useTranslation(
    ["education/programmingcontest/problem", "common"]
  );

  const classes = useStyles();

  return (
    <div className={classes.fileContainer}>
      <div className={classes.fileWrapper}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            p: 1,
            borderRadius: 2,
            transition: 'background-color 0.2s ease-in-out',
            '&:hover': {
              backgroundColor: grey[100],
            },
          }}
        >
          <Box sx={{display: 'flex', alignItems: 'center', flex: 1, minWidth: 0}}>
            {getFileIcon(file.fileName)}
            <Typography
              variant="body2"
              sx={{
                ml: 1.5,
                flex: 1,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                fontWeight: 500,
              }}
            >
              {file.fileName}
            </Typography>
          </Box>
          <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
            {downloadable && (
              <Tooltip title={t("common:download")}>
                <IconButton
                  onClick={() => {
                    if (onDownload) {
                      // Custom download handler (ưu tiên)
                      onDownload(file);
                    } else if (file.content) {
                      // File có content (base64) - download trực tiếp
                      saveByteArray(
                        file.fileName,
                        file.content,
                        getFileType(file.fileName)
                      );
                    }
                  }}
                  size="small"
                  sx={{color: blue[700]}}
                >
                  <DownloadIcon/>
                </IconButton>
              </Tooltip>
            )}
            {removable && (
              <Tooltip title={t("common:delete")}>
                <IconButton
                  onClick={onRemove}
                  size="small"
                  sx={{color: red[700]}}
                >
                  <DeleteIcon/>
                </IconButton>
              </Tooltip>
            )}
          </Box>
        </Box>
      </div>
    </div>
  )
}

export default FileUploadZone;
