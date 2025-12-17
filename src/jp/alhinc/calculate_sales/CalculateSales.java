package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";
	
	//商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	//商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";
	
	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String COMMODITY_FILE_NOT_EXIST = "商品定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String COMMODITY_FILE_INVALID_FORMAT = "商品定義ファイルのフォーマットが不正です";
	private static final String SALES_FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String FILE_NOT_SEQUENTIAL_NUMBER = "売上ファイル名が連番になっていません";
	private static final String SALES_AMOUNT_ERROR = "売上合計金額が10桁を超えました";
	private static final String COMMODITY_SALES_AMOUNT_ERROR = "商品ごとの売上合計金額が10桁を超えました";
	private static final String BRANCH_CODE_INVALID = "の支店コードが不正です";
	private static final String COMMODITY_CODE_INVALID = "の商品コードが不正です";
	
	private static final String BRANCH_CODE_REGEX = "^[0-9]{3}$";
	private static final String COMMODITY_CODE_REGEX = "^[a-zA-Z0-9]{8}$";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		//コマンドライン引数が渡されているか確認(エラー処理内容3)
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}
		
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		//商品コードと商品名を保持するMap
		Map<String, String>commodityNames = new HashMap<>();
		//商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, BRANCH_CODE_REGEX, FILE_NOT_EXIST, FILE_INVALID_FORMAT )) {
			return;
		}
		//商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, COMMODITY_CODE_REGEX, COMMODITY_FILE_NOT_EXIST, COMMODITY_FILE_INVALID_FORMAT)) {
			return;
		}

		// 集計処理
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();
		BufferedReader br = null;

		for(int i = 0; i < files.length; i++) {
			String filesName = files[i].getName();
			//ファイルなのか、ファイル名が数字8桁かどうかを確認
			if(files[i].isFile() && filesName.matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}			
		}

		// 売上ファイルが連番か確認	
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() - 1; i++ ) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8)); 
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8)); 
			if((latter - former) != 1) {
				System.out.println(FILE_NOT_SEQUENTIAL_NUMBER);
				return;
			}
		}
		try {
			for(int i = 0; i < rcdFiles.size(); i++) {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);
				List<String> filesContents = new ArrayList<>();

				String line;
				while((line = br.readLine()) != null) {
					filesContents.add(line);
				}
				//売上ファイルのフォーマットを確認
				if(filesContents.size() != 3) {
					System.out.println(rcdFiles.get(i).getName() + SALES_FILE_INVALID_FORMAT);
					return;
				}
				
				//Mapに特定のKeyが存在するか確認(店舗コード)
				if(!branchNames.containsKey(filesContents.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + BRANCH_CODE_INVALID);
					return;
				}
				
				//Mapに特定のKeyが存在するか確認（商品コード）
				if(!commodityNames.containsKey(filesContents.get(1))) {
					System.out.println(rcdFiles.get(i).getName() + COMMODITY_CODE_INVALID);
					return;
				}

				
				//売上金額が数字なのか確認
				if(!filesContents.get(2).matches("^\\d+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}
				
				long fileSale = Long.parseLong(filesContents.get(2));
				long saleAmount = branchSales.get(filesContents.get(0)) + fileSale;
				long commoditySaleAmount = commoditySales.get(filesContents.get(1)) + fileSale;
				
				//売上金額の合計が10桁を超えたか確認
				if(saleAmount >= 10000000000L){ 
					System.out.println(SALES_AMOUNT_ERROR);
					return;
				} 
				
				//商品ごとの売上金額の合計が10桁を超えたか確認 
				if(commoditySaleAmount >= 10000000000L){ 
					System.out.println(COMMODITY_SALES_AMOUNT_ERROR);
					return;
				} 
				
				branchSales.put(filesContents.get(0), saleAmount);
				commoditySales.put(filesContents.get(1), commoditySaleAmount);

			}
		}catch(IOException e){
			System.out.println(UNKNOWN_ERROR);
			return;
		}finally {
			if(br != null) {
				try {
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		
		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル、商品定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @param 商品コードと商品名を保持するMap
	 * @param 商品コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> Names, Map<String, Long> Sales, String Regex, String notExistMessage, String invalidFormatMessage) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			// ファイルの存在チェック
			if(!file.exists()) {
				System.out.println(notExistMessage);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				String [] items = line.split(",");
				
				// ファイルのフォーマットチェック
				if(items.length != 2 || !items[0].matches(Regex)) {
					System.out.println(invalidFormatMessage);
					return false;
				}
				
				Names.put(items[0],items[1]);
				Sales.put(items[0], 0L);
			}	

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> Names, Map<String, Long> Sales) {
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

            for(String key : Names.keySet()) {
            	bw.write(key + "," + Names.get(key) + "," + Sales.get(key));
            	bw.newLine();
            }
			
		}catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		}finally {
			if(bw != null) {
				try {
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}