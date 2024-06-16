package sumcoda.webide.workspace.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sumcoda.webide.workspace.enumerate.Language;

@Slf4j
@Getter
@NoArgsConstructor
public class WorkspaceResponseDAO {

    private Long id;
    private String title;
    private Language language;
    private String description;

    private String nickname;

    private String awsS3SavedFileURL;

    @Builder
    public WorkspaceResponseDAO(Long id, String title, Language language, String description, String nickname, String awsS3SavedFileURL) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.description = description;
        this.nickname = nickname;
        this.awsS3SavedFileURL = awsS3SavedFileURL;
    }
}
