package vn.edu.hust.soict.judge0client.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import vn.edu.hust.soict.judge0client.config.Judge0Config;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Judge0Utils {

    Judge0Config judge0Config;

    public boolean isMultiThreadedProgram(int languageId, String sourceCode) {
        return (languageId == 71 && sourceCode.contains("ortools")) || languageId == 62;
    }

    public Judge0Config.ServerConfig getServerConfig(int languageId, String sourceCode) {
        if (isMultiThreadedProgram(languageId, sourceCode)) {
            return judge0Config.getMultiThreaded();
        } else {
            return judge0Config.getSingleThreaded();
        }
    }

    public Integer getMaxProcessesAndOrThreads(int languageId, String sourceCode) {
        if (isMultiThreadedProgram(languageId, sourceCode)) {
            return null;
        } else {
            return languageId == 62 ? judge0Config.getMultiThreaded().getSubmission().getJavaMaxProcessesAndOrThreads() : Integer.valueOf(2);
        }
    }
}
