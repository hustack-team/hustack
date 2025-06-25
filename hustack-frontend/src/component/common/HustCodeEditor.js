import React from "react";
import AceEditor from "react-ace";
import { Box, Typography } from "@mui/material";
import HustCodeLanguagePicker from "./HustCodeLanguagePicker";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/mode-c_cpp";
import "ace-builds/src-noconflict/mode-python";
import "ace-builds/src-noconflict/theme-monokai";
import { COMPUTER_LANGUAGES } from "../education/programmingcontestFE/Constant";

const HustCodeEditor = (props) => {
  const {
    classRoot,
    title,
    placeholder = "Write your source code here",
    hidePlaceholder = false,
    language,
    onChangeLanguage,
    listLanguagesAllowed,
    sourceCode,
    onChangeSourceCode,
    height = "420px",
    hideLanguagePicker,
    readOnly = false,
    hideProgrammingLanguage,
    blockEditor,
    isStudentBlock = false,
    theme = "monokai",
    maxLines,
    minLines,
    ...remainProps
  } = props;

  const convertLanguageToEditorMode = (language) => {
    switch (language) {
      case COMPUTER_LANGUAGES.C:
      case COMPUTER_LANGUAGES.CPP11:
      case COMPUTER_LANGUAGES.CPP14:
      case COMPUTER_LANGUAGES.CPP17:
        return "c_cpp";
      case COMPUTER_LANGUAGES.JAVA:
        return "java";
      case COMPUTER_LANGUAGES.PYTHON:
        return "python";
      default:
        return "c_cpp";
    }
  };

  const calculatedMinLines = minLines ?? (blockEditor === 1 ? 8 : 40);
  const editorClassName = isStudentBlock ? "student-block-editor" : "";

  return (
    <>
      {isStudentBlock && (
        <style>
          {`
            .student-block-editor .ace_gutter {
              background-color: rgb(189, 189, 189) !important;
            }
          `}
        </style>
      )}
      <Box {...remainProps} className={`${classRoot}`} sx={{ marginTop: "24px" }}>
        {hideProgrammingLanguage !== 1 && language && (
          <Box
            sx={{
              display: "flex",
              flexDirection: "row",
              justifyContent: "space-between",
              marginBottom: "8px",
            }}
          >
            <Typography variant="h6">{title}</Typography>
            {!hideLanguagePicker && (
              <HustCodeLanguagePicker
                listLanguagesAllowed={listLanguagesAllowed}
                language={language}
                onChangeLanguage={onChangeLanguage}
              />
            )}
          </Box>
        )}
        <Box
          sx={{
            minHeight: "120px",
            overflowY: "auto",
            padding: "8px",
            borderRadius: "4px",
          }}
        >
          <AceEditor
            width="100%"
            height={maxLines || minLines ? undefined : height}
            style={{
              paddingTop: "6px",
              padding: "8px",
              minHeight: "120px",
              overflowY: "auto",
            }}
            placeholder={hidePlaceholder ? "" : placeholder}
            mode={convertLanguageToEditorMode(language)}
            theme={theme}
            onChange={onChangeSourceCode}
            fontSize={16}
            value={sourceCode}
            maxLines={maxLines}
            minLines={calculatedMinLines}
            readOnly={readOnly}
            className={editorClassName}
          />
        </Box>
      </Box>
    </>
  );
};

export default React.memo(HustCodeEditor);
