import React from "react";
import AceEditor from "react-ace";
import {Box, Typography} from "@mui/material";
import HustCodeLanguagePicker from "./HustCodeLanguagePicker";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/mode-c_cpp";
import "ace-builds/src-noconflict/mode-python";
import "ace-builds/src-noconflict/theme-monokai";
import "ace-builds/src-noconflict/theme-github";
import "ace-builds/src-noconflict/theme-github_light_default";
import {COMPUTER_LANGUAGES} from "../education/programmingcontestFE/Constant";

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

const HustCodeEditor = (props) => {
  const {
    classRoot,
    title,
    placeholder,
    language,
    onChangeLanguage,
    listLanguagesAllowed,
    sourceCode,
    onChangeSourceCode,
    height = "420px",
    hideLanguagePicker,
    readOnly = false,
    hideProgrammingLanguage,
    minLines = 20,
    ...remainProps
  } = props;

  return (
    <Box {...remainProps} className={`${classRoot}`}>
      {hideProgrammingLanguage !== 1 && (
        <Box sx={{display: "flex", flexDirection: "row", justifyContent: "space-between", marginBottom: "8px"}}>
          <Typography variant="h6">{title}</Typography>
          {language && !hideLanguagePicker && (
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
          // padding: "8px",
          // borderRadius: "4px",
        }}
      >
        <AceEditor
          width="100%"
          height={height}
          minLines={minLines}
          maxLines={Infinity}
          style={{
            paddingTop: "6px",
            padding: "8px",
            minHeight: "120px",
            overflowY: "auto",
          }}
          placeholder={placeholder}
          mode={convertLanguageToEditorMode(language)}
          theme={props.theme || "monokai"}
          onChange={onChangeSourceCode}
          fontSize={16}
          value={sourceCode}
          readOnly={readOnly}
        />
      </Box>
    </Box>
  );
};

export default React.memo(HustCodeEditor);