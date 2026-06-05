package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import java.util.List;
import java.util.Optional;

public interface OpenAccountRepository {
    OpenAccount save(OpenAccount openAccount);
    Optional<OpenAccount> findById(Long id);
    List<OpenAccount> findAll();
    PageResult<OpenAccount> search(SearchOpenAccountsCommand command);
    void delete(Long id);
    int reactivate(Long id);
}
