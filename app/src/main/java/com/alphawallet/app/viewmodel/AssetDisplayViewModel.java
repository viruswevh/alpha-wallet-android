package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.RedeemAssetSelectRouter;
import com.alphawallet.app.router.TransferTicketRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.ConfirmationActivity;
import com.alphawallet.app.ui.RedeemAssetSelectActivity;
import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.app.ui.TransferTicketDetailActivity;
import com.alphawallet.app.ui.widget.entity.TicketRangeParcel;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayViewModel extends BaseViewModel
{
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final String TAG = "ADVM";
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final TransferTicketRouter transferTicketRouter;
    private final RedeemAssetSelectRouter redeemAssetSelectRouter;
    private final MyAddressRouter myAddressRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final OpenseaService openseaService;
    private final TokensService tokensService;

    private Token refreshToken;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Token> ticket = new MutableLiveData<>();
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;

    AssetDisplayViewModel(
            FetchTokensInteract fetchTokensInteract,
            GenericWalletInteract genericWalletInteract,
            TransferTicketRouter transferTicketRouter,
            RedeemAssetSelectRouter redeemAssetSelectRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            MyAddressRouter myAddressRouter,
            AssetDefinitionService assetDefinitionService,
            OpenseaService openseaService,
            TokensService tokensService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.redeemAssetSelectRouter = redeemAssetSelectRouter;
        this.transferTicketRouter = transferTicketRouter;
        this.myAddressRouter = myAddressRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.openseaService = openseaService;
        this.tokensService = tokensService;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<Token> ticket() {
        return ticket;
    }
    public LiveData<XMLDsigDescriptor> sig() { return sig; }

    public void selectAssetIdsToRedeem(Context context, Token token) {
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        redeemAssetSelectRouter.open(context, token);
    }

    public OpenseaService getOpenseaService()
    {
        return openseaService;
    }

    private void fetchCurrentTicketBalance() {
        if (getBalanceDisposable != null) getBalanceDisposable.dispose();
        if (ticket().getValue() != null && !ticket().getValue().independentUpdate())
        {
            getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> fetchTokensInteract
                            .fetchSingle(defaultWallet.getValue(), ticket().getValue())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onToken, this::onError, this::finishTokenFetch)).subscribe();
        }
    }

    private void finishTokenFetch()
    {
        Log.d(TAG, "refreshToken: " + refreshToken.tokenInfo.name );
        ticket.postValue(refreshToken);
    }

    public void prepare(Token t) {
        ticket.setValue(t);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    private void onToken(Token t)
    {
        refreshToken = t;
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void showTransferToken(Context context, Token ticket) {
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        transferTicketRouter.open(context, ticket);
    }

    public void showContractInfo(Context ctx, Token token)
    {
        myAddressRouter.open(ctx, defaultWallet.getValue(), token);
    }

    public void sellTicketRouter(Context context, Token token, String tokenIds) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, defaultWallet.getValue());
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.EXTRA_TOKENID_LIST, tokenIds);
        intent.putExtra(C.EXTRA_STATE, SellDetailActivity.SET_A_PRICE);
        intent.putExtra(C.EXTRA_PRICE, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    private void onDefaultWallet(Wallet wallet) {
        //TODO: switch on 'use' button
        progress.postValue(false);
        defaultWallet.setValue(wallet);
        fetchCurrentTicketBalance();
    }

    public void selectRedeemTokens(Context ctx, Token token, List<BigInteger> idList)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(idList, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemAssetSelectActivity.class);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.Key.TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void showTransferToken(Context ctx, Token token, List<BigInteger> selection)
    {
        Intent intent = new Intent(ctx, TransferTicketDetailActivity.class);
        intent.putExtra(C.Key.WALLET, defaultWallet.getValue());
        intent.putExtra(C.Key.TICKET, token);

        intent.putExtra(C.EXTRA_TOKENID_LIST, token.bigIntListToString(selection, false));

        if (token.isERC721()) //skip numerical selection - ERC721 has no multiple token transfer
        {
            intent.putExtra(C.EXTRA_STATE, TRANSFER_TO_ADDRESS.ordinal());
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void checkTokenScriptValidity(Token token)
    {
        disposable = assetDefinitionService.getSignatureData(token.tokenInfo.chainId, token.tokenInfo.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig::postValue, this::onSigCheckError);
    }

    private void onSigCheckError(Throwable throwable)
    {
        XMLDsigDescriptor failSig = new XMLDsigDescriptor();
        failSig.result = "fail";
        failSig.type = SigReturnType.NO_TOKENSCRIPT;
        failSig.subject = throwable.getMessage();
        sig.postValue(failSig);
    }

    public Token getCurrency(int chainId)
    {
        return tokensService.getToken(chainId, defaultWallet.getValue().address);
    }

    public String getTransactionBytes(Token token, BigInteger tokenId, FunctionDefinition def)
    {
        return assetDefinitionService.generateTransactionPayload(token, tokenId, def);
    }

    public void confirmTransaction(Context ctx, int networkId, String functionData, String toAddress,
                                   String contractAddress, String additionalDetails, String functionName, String value)
    {
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TRANSACTION_DATA, functionData);
        intent.putExtra(C.EXTRA_NETWORKID, networkId);
        intent.putExtra(C.EXTRA_NETWORK_NAME, findDefaultNetworkInteract.getNetworkName(networkId));
        intent.putExtra(C.EXTRA_AMOUNT, value);
        if (toAddress != null) intent.putExtra(C.EXTRA_TO_ADDRESS, toAddress);
        if (contractAddress != null) intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_ACTION_NAME, additionalDetails);
        intent.putExtra(C.EXTRA_FUNCTION_NAME, functionName);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.TOKENSCRIPT.ordinal());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public void confirmNativeTransaction(Context ctx, String toAddress, BigDecimal value, Token nativeEth, String info)
    {
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, toAddress);
        intent.putExtra(C.EXTRA_AMOUNT, value.toString());
        intent.putExtra(C.EXTRA_DECIMALS, nativeEth.tokenInfo.decimals);
        intent.putExtra(C.EXTRA_SYMBOL, nativeEth.getSymbol());
        intent.putExtra(C.EXTRA_SENDING_TOKENS, false);
        intent.putExtra(C.EXTRA_ENS_DETAILS, info);
        intent.putExtra(C.EXTRA_NETWORKID, nativeEth.tokenInfo.chainId);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.ETH.ordinal());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }
}
