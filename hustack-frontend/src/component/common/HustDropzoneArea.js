import React, { useCallback } from "react";
import { DropzoneArea } from "material-ui-dropzone";
import { useTranslation } from "react-i18next";
import Box from "@mui/material/Box";
import { makeStyles } from "@material-ui/core";
import { Typography } from "@mui/material";

const useStyles = makeStyles((theme) => ({
  dropzone: {
    minHeight: "100px",
    borderRadius: "16px",
    marginBottom: theme.spacing(1),
    whiteSpace: "pre-line",
    "& .MuiDropzoneArea-text": {
      fontSize: "14px !important",
      lineHeight: "1.4 !important",
    },
  },
  dropzoneWithInfo: {
    minHeight: "100px",
    borderRadius: "16px",
    marginBottom: theme.spacing(1),
    whiteSpace: "pre-line",
    "& .MuiDropzoneArea-text": {
      fontSize: "15px !important",
      lineHeight: "1.1 !important",
    },
  },
  hideLogo: {
    "& .MuiDropzoneArea-icon": {
      display: "none",
    },
  },
}));

const HustDropzoneArea = React.forwardRef((props, ref) => {
  const {
    title,
    onChangeAttachment,
    classRoot,
    hideFileList = false,
    hideLogo = false,
    acceptedFiles,
    maxFileSize,
    filesLimit,
    initialFiles = [],
    showAlerts = true,
    ...remainProps
  } = props;

  const { t } = useTranslation("common");
  const classes = useStyles();

  let dropzoneText = t("dropzone.dropzoneTextDefault");
  const hasFileRestrictions = acceptedFiles && maxFileSize && filesLimit;

  if (hasFileRestrictions) {
    const readableExtensions = acceptedFiles
      .map((ext) => ext.replace(".", ""))
      .join(", ");
    const readableMaxSizeMB = Math.floor(maxFileSize / (1024 * 1024));
    dropzoneText = t("dropzone.dropzoneTextWithInfo", {
      types: readableExtensions,
      maxSize: readableMaxSizeMB,
      maxFiles: filesLimit,
    });
  }

  const handleChange = useCallback(
    (files) => {
      console.log("DropzoneArea handleChange:", files);
      if (onChangeAttachment) {
        onChangeAttachment(files, []);
      }
    },
    [onChangeAttachment]
  );

  const handleDrop = useCallback(
    (acceptedFiles, rejectedFiles) => {
      console.log("DropzoneArea handleDrop:", { acceptedFiles, rejectedFiles });
      if (onChangeAttachment) {
        onChangeAttachment(acceptedFiles, rejectedFiles);
      }
    },
    [onChangeAttachment]
  );

  return (
    <Box className={`${classRoot} ${hideLogo ? classes.hideLogo : ""}`}>
      <Typography
        variant="h6"
        display="block"
        style={{ margin: "24px 0 8px 0px", width: "100%" }}
      >
        {title ? title : t("dropzone.title")}
      </Typography>
      <DropzoneArea
        {...remainProps}
        ref={ref}
        acceptedFiles={acceptedFiles}
        maxFileSize={maxFileSize}
        filesLimit={filesLimit}
        initialFiles={initialFiles}
        dropzoneClass={
          hasFileRestrictions ? classes.dropzoneWithInfo : classes.dropzone
        }
        showPreviews={!hideFileList}
        showPreviewsInDropzone={false}
        useChipsForPreview={!hideFileList}
        dropzoneText={dropzoneText}
        previewText={t("dropzone.previewText")}
        previewChipProps={{
          variant: "outlined",
          color: "primary",
          size: "medium",
          ...props.previewChipProps,
        }}
        getFileAddedMessage={(fileName) =>
          t("dropzone.getFileAddedMessage", { fileName })
        }
        getFileRemovedMessage={(fileName) =>
          t("dropzone.getFileRemovedMessage", { fileName })
        }
        getFileLimitExceedMessage={(filesLimit) =>
          t("dropzone.getFileLimitExceedMessage", { filesLimit })
        }
        alertSnackbarProps={{
          anchorOrigin: { vertical: "bottom", horizontal: "right" },
          autoHideDuration: 1800,
        }}
        showAlerts={showAlerts}
        onChange={handleChange}
        onDrop={handleDrop}
      />
    </Box>
  );
});

export default React.memo(HustDropzoneArea);
