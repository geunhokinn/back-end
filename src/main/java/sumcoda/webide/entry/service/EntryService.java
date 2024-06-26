package sumcoda.webide.entry.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sumcoda.webide.entry.domain.Entry;
import sumcoda.webide.entry.dto.request.EntryCreateRequestDTO;
import sumcoda.webide.entry.dto.request.EntryRenameRequestDTO;
import sumcoda.webide.entry.dto.request.EntrySaveRequestDTO;
import sumcoda.webide.entry.exception.*;
import sumcoda.webide.entry.repository.EntryRepository;
import sumcoda.webide.memberworkspace.enumerate.MemberWorkspaceRole;
import sumcoda.webide.workspace.domain.Workspace;
import sumcoda.webide.workspace.dto.response.WorkspaceEntriesResponseDTO;
import sumcoda.webide.workspace.enumerate.Status;
import sumcoda.webide.workspace.exception.WorkspaceAccessException;
import sumcoda.webide.workspace.exception.WorkspaceFoundException;
import sumcoda.webide.workspace.repository.WorkspaceRepository;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntryService {
    private final EntryRepository entryRepository;
    private final WorkspaceRepository workspaceRepository;

    // 엔트리 생성(디렉토리 또는 파일을 생성)
    @Transactional
    public WorkspaceEntriesResponseDTO createEntry(Long workspaceId, Long parentId, EntryCreateRequestDTO entryCreateRequestDTO, String username) {

        String entryName = entryCreateRequestDTO.getName();
        boolean isDirectory = entryCreateRequestDTO.getIsDirectory();

        // 워크스페이스가 존재하는지 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceFoundException("존재하지 않는 워크스페이스 Id 입니다.: " + workspaceId));

        // 유저가 엔트리에 권한이 존재하는지 확인
        checkUserAccessToEntry(workspace, username);

        // 상태가 DEFAULT 가 아니면 워크스페이스를 수정할 수 없음
        if (workspace.getStatus() != Status.DEFAULT) {
            throw new EntryUpdateException("완료된 컨테이너나 해결된 컨테이너의 엔트리를 수정할 수 없습니다.");
        }

        // 워크스페이스 안에 엔트리가 존재하는지 확인
        Entry parentEntry = entryRepository.findByWorkspaceIdAndId(workspaceId, parentId)
                .orElseThrow(() -> new EntryFoundException("워크스페이스에 존재하지 않는 엔트리 Id 입니다.: " + parentId));

        // 엔트리가 디렉토리인지 확인
        if (Boolean.FALSE.equals(parentEntry.getIsDirectory())) {
            throw new EntryCreateException("파일에 디렉토리를 생성할 수 없습니다.");
        }

        // 파일 또는 디렉토리 이름의 형식을 확인
        if (entryName.contains(".") && isDirectory) {
            throw new EntryAccessException("디렉토리 이름에는 '.' 문자가 포함될 수 없습니다.");
        } else if (!entryName.contains(".") && !isDirectory) {
            throw new EntryAccessException("파일 이름에는 '.' 문자가 포함되어야 합니다.");
        }

        // 부모 디렉토리 안에 같은 이름의 엔트리가 존재하는지 확인
        if (entryRepository.findByParentAndName(parentEntry, entryName).isPresent()) {
            throw new EntryAlreadyExistsException("같은 이름의 엔트리가 이미 존재합니다.: " + entryName);
        }

        // 새 엔트리 생성
        Entry entry = Entry.createEntry(
                entryName,
                null,
                isDirectory,
                parentEntry,
                workspace
        );

        // 새 엔트리를 저장
        entryRepository.save(entry);

        //엔트리를 DTO 로 변환하여 반환
        return workspaceRepository.findAllEntriesByWorkspaceId(workspaceId);
    }

    // 엔트리 삭제
    @Transactional
    public WorkspaceEntriesResponseDTO deleteEntry(Long workspaceId, Long entryId, String username) {

        // 워크스페이스가 존재하는지 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceFoundException("존재하지 않는 워크스페이스 Id 입니다.: " + workspaceId));

        // 유저가 엔트리에 권한이 존재하는지 확인
        checkUserAccessToEntry(workspace, username);

        // 상태가 DEFAULT 가 아니면 워크스페이스를 수정할 수 없음
        if (workspace.getStatus() != Status.DEFAULT) {
            throw new EntryUpdateException("완료된 컨테이너나 해결된 컨테이너의 엔트리를 수정할 수 없습니다.");
        }

        // 워크스페이스 안에 엔트리가 존재하는지 확인
        Entry entry = entryRepository.findByWorkspaceIdAndId(workspaceId, entryId)
                .orElseThrow(() -> new EntryFoundException("워크스페이스에 존재하지 않는 엔트리 Id 입니다.: " + entryId));

        // 최상위 디렉토리인지 확인
        if (entry.getParent() == null) {
            throw new RootEntryDeleteException("최상위 디렉토리는 삭제할 수 없습니다.");
        }

        // 엔트리 삭제
        entryRepository.delete(entry);

        //엔트리를 DTO 로 변환하여 반환
        return workspaceRepository.findAllEntriesByWorkspaceId(workspaceId);
    }

    // 엔트리 이름 수정
    @Transactional
    public void renameEntry(Long workspaceId, Long entryId, EntryRenameRequestDTO entryRenameRequestDTO, String username) {

        String newName = entryRenameRequestDTO.getName();
        boolean isDirectory = entryRenameRequestDTO.getIsDirectory();

        // 워크스페이스가 존재하는지 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceFoundException("존재하지 않는 워크스페이스 Id 입니다.: " + workspaceId));

        // 유저가 엔트리에 권한이 존재하는지 확인
        checkUserAccessToEntry(workspace, username);

        // 상태가 DEFAULT 가 아니면 워크스페이스를 수정할 수 없음
        if (workspace.getStatus() != Status.DEFAULT) {
            throw new EntryUpdateException("완료된 컨테이너나 해결된 컨테이너의 엔트리를 수정할 수 없습니다.");
        }

        // 워크스페이스 안에 엔트리가 존재하는지 확인
        Entry entry = entryRepository.findByWorkspaceIdAndId(workspaceId, entryId)
                .orElseThrow(() -> new EntryFoundException("존재하지 않는 엔트리 Id 입니다.: " + entryId));

        // 엔트리의 타입 변경 요청인지 확인
        if (isDirectory != Boolean.TRUE.equals(entry.getIsDirectory())) {
            throw new EntryAccessException("엔트리 타입을 변경할 수 없습니다.");
        }

        // 파일 또는 디렉토리 이름의 형식을 확인
        if (newName.contains(".") && isDirectory) {
            throw new EntryAccessException("디렉토리 이름에는 '.' 문자가 포함될 수 없습니다.");
        } else if (!newName.contains(".") && !isDirectory) {
            throw new EntryAccessException("파일 이름에는 '.' 문자가 포함되어야 합니다.");
        }

        // 최상위 디렉토리인지 확인
        if (entry.getParent() == null) {
            throw new RootEntryDeleteException("최상위 디렉토리는 수정할 수 없습니다.");
        }

        // 디렉토리 안에 같은 이름의 엔트리가 존재하는지 확인
        if (entryRepository.findByParentAndName(entry.getParent(), newName).isPresent()) {
            throw new EntryAlreadyExistsException("같은 이름의 엔트리가 이미 존재합니다.: " + newName);
        }

        // 디렉토리 이름 업데이트
        entry.updateName(newName);
    }

    // 엔트리 내용 저장
    @Transactional
    public void saveEntry(Long workspaceId, Long entryId, EntrySaveRequestDTO entrySaveRequestDTO, String username) {

        // 워크스페이스가 존재하는지 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceFoundException("존재하지 않는 워크스페이스 Id 입니다.: " + workspaceId));

        // 유저가 엔트리에 권한이 존재하는지 확인
        checkUserAccessToEntry(workspace, username);

        // 상태가 DEFAULT 가 아니면 워크스페이스를 수정할 수 없음
        if (workspace.getStatus() != Status.DEFAULT) {
            throw new EntryUpdateException("완료된 컨테이너나 해결된 컨테이너의 엔트리를 수정할 수 없습니다.");
        }

        // 워크스페이스 안에 엔트리가 존재하는지 확인
        Entry entry = entryRepository.findByWorkspaceIdAndId(workspaceId, entryId)
                .orElseThrow(() -> new EntryFoundException("워크스페이스에 존재하지 않는 파일 Id 입니다.: " + entryId));

        // 엔트리가 파일인지 확인
        if (Boolean.TRUE.equals(entry.getIsDirectory())) {
            throw new EntryAccessException("디렉토리는 내용을 저장할 수 없습니다.");
        }

        // 파일 내용 업데이트
        entry.updateContent(entrySaveRequestDTO.getContent());
        // entryRepository.save(entry); 호출 없이도 트랜잭션이 커밋될 때 변경 사항이 반영됨.(dirty checking)
    }

    // 공통 메서드

    // 유저가 엔트리에 접근 권한이 존재하는지 확인
    private void checkUserAccessToEntry(Workspace workspace, String username) {

        // private 워크스페이스일 때, Admin 이 아닌 유저가 접근하려고 하면 예외 발생
        if (!workspace.getIsPublic()) {
            boolean isAdmin = workspace.getMemberWorkspaces().stream()
                    .anyMatch(mw -> mw.getMember().getUsername().equals(username) && mw.getRole().equals(MemberWorkspaceRole.ADMIN));
            if (!isAdmin) {
                throw new WorkspaceAccessException("해당 컨테이너에 접근 권한이 없습니다.: " + username);
            }
        }
    }
}
