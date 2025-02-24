  var
    N, i, val, sum: Integer;
    arr: array of Integer;
  begin
    Write('Введите количество чисел ');
    Read(N);
    write;
    SetLength(arr, N);
    
    Writeln('Введите числа: ');
    for i := 0 to N - 1 do
      Read(arr[i]);
      write;
    sum := N;
    
    for i := 0 to N - 2 do
      for var j := 0 to N - 2  do
        if arr[j] >= arr[j + 1] then
        begin
          var temp := arr[j];
          arr[j] := arr[j + 1];
          arr[j + 1] := temp;
        end;
        var min:Integer := arr[0]*2; 
   for i := 1 to N-1 do
       if (min < arr[i]) then
       begin
          sum -= 1;
       end;
       if sum = N then
        sum := 0;
       writeln('Сумма => ',sum);
  end.