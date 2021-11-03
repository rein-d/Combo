package com.rein.android.combo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.evotor.framework.component.PaymentDelegatorApi;
import ru.evotor.framework.core.IntegrationException;
import ru.evotor.framework.core.IntegrationManagerCallback;
import ru.evotor.framework.core.IntegrationManagerFuture;
import ru.evotor.framework.core.action.command.open_receipt_command.OpenSellReceiptCommand;
import ru.evotor.framework.core.action.event.receipt.changes.position.PositionAdd;
import ru.evotor.framework.receipt.Position;
import ru.evotor.framework.receipt.Receipt;
import ru.evotor.framework.receipt.ReceiptApi;
import ru.evotor.framework.receipt.formation.api.ReceiptFormationCallback;
import ru.evotor.framework.receipt.formation.api.ReceiptFormationException;
import ru.evotor.framework.receipt.formation.api.SellApi;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.OpenSellReceiptButton).setOnClickListener(view -> {
            openSellReceiptCombo();
        });


    }

    private void openSellReceiptCombo(){
        List<PositionAdd> positionAddList = new ArrayList<>();
        positionAddList.add(
                new PositionAdd(
                        Position.Builder.newInstance(
                                //UUID позиции
                                UUID.randomUUID().toString(),
                                //UUID товара
                                UUID.randomUUID().toString(),
                                //Наименование
                                "Тестовый Товар",
                                //Наименование единицы измерения
                                "кг",
                                //Точность единицы измерения
                                0,
                                //Цена без скидок
                                new BigDecimal(30000),
                                //Количество
                                BigDecimal.valueOf(1,1)
                                //Добавление цены с учетом скидки на позицию. Итог = price - priceWithDiscountPosition
                        )
                                .build()

                )
        );
        new OpenSellReceiptCommand(positionAddList, null).process(this, new IntegrationManagerCallback() {
            @Override
            public void run(IntegrationManagerFuture future) {

                try {
                    IntegrationManagerFuture.Result result = future.getResult();
                    if (result.getType() == IntegrationManagerFuture.Result.Type.OK) {

                        Receipt MyReceipt124 = ReceiptApi.getReceipt(MainActivity.this, Receipt.Type.SELL);
                        MyReceipt124.getPrintDocuments();
                        String Receipt_uuid = MyReceipt124.getHeader().getUuid();
                        //Toast.makeText(MainActivity.this, Receipt_uuid, Toast.LENGTH_LONG).show();
                        SellApi.moveCurrentReceiptDraftToPaymentStage(
                                MainActivity.this,
                                PaymentDelegatorApi.INSTANCE.getAllPaymentDelegators(MainActivity.this.getPackageManager()).get(0),
                                new ReceiptFormationCallback() {
                                    public void onError(ReceiptFormationException error) {
                                    }

                                    public void onSuccess() {
                                    }

                                }
                        );
                    }
                } catch (IntegrationException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Есть проблема: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}