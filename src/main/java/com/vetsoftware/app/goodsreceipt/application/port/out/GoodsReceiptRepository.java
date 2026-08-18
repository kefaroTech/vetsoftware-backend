package com.vetsoftware.app.goodsreceipt.application.port.out;

import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import java.util.List;
import java.util.Optional;

public interface GoodsReceiptRepository {
    GoodsReceipt save(GoodsReceipt goodsReceipt);

    Optional<GoodsReceipt> findByIdAndCompanyId(Long id, Long companyId);

    List<GoodsReceipt> findAllByCompanyId(Long companyId);

    PageResult<GoodsReceipt> search(SearchGoodsReceiptsCommand command);

    /**
     * Baja lógica acotada al tenant. El {@code companyId} viaja hasta el
     * {@code WHERE} del UPDATE: la lectura previa del caso de uso valida la
     * propiedad, pero el filtro del SQL es lo que la sostiene si alguien reordena
     * el servicio o llama al adaptador desde otro sitio.
     */
    void delete(Long id, Long companyId);
}
